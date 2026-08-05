# Yzkj.Novanet 源码逻辑梳理

## 一、项目概述

**Yzkj.Novanet** 是一款针对 **Nova POCT（即时检验）血糖分析仪** 的**上位机管理系统**，以 Windows 服务形式运行，通过 TCP/XML 协议（DML）与 Nova 设备进行双向通信，实现：

- 设备接入认证与状态管理（设备自动注册、时间戳比对）
- 血糖结果（OBS.R01）与质控结果（OBS.R02）的上行接收与去重入库
- 设备配置（Setup）、科室（Location）、护士（Operator）、患者（Patient）、试剂（Reagent）的增量下行同步
- 与医院信息系统（DMS/HIS）的双向数据同步（通过存储过程）
- 基于 ASP.NET Identity 的 Web 管理后台（用户认证、角色权限）

**服务名称**：`Nova Protocol Service`，默认监听端口 `57380`。

---

## 二、技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | .NET Framework 4.5.2 | 目标运行时 |
| ORM | Entity Framework 6 | Code First + 自动迁移 |
| 身份认证 | ASP.NET Identity 2.x | OWIN + Cookie 认证 |
| IoC 容器 | Autofac | 注册 DbContext 和业务类 |
| 服务宿主 | Topshelf | 将类库注册为 Windows 服务 |
| 日志 | NLog | 结构化日志（写入 NovaLog 表） |
| 动态排序 | System.Linq.Dynamic | 支持字符串表达式排序 |
| 序列化 | XmlSerializer | 配置 XML 读写 |
| 数据库 | SQL Server | 连接字符串键：`DefaultConnection` |

---

## 三、完整目录结构

```
c#/
├── Properties/
│   └── AssemblyInfo.cs                  # 程序集元信息
│
├── Yzkj.Novanet.Bussiness/              # 业务逻辑层（核心 CRUD）
│   ├── AppUserManager.cs                # 用户管理器（Identity）
│   ├── AppSignInManager.cs              # 登录管理器（Identity）
│   ├── DeviceBus.cs                     # 设备 CRUD
│   ├── DiagcodeBus.cs                   # 诊断项目 CRUD + 分组
│   ├── DischargeClockBus.cs             # 出院定时任务
│   ├── LocationBus.cs                  # 科室/医院树 CRUD（聚合根）
│   ├── NovaLogBus.cs                    # 系统日志查询
│   ├── NovaSetupBus.cs                  # Nova 设备配置管理（13 组配置方法）
│   ├── NurseBus.cs                      # 护士/操作员 CRUD + 事务
│   ├── PatientBus.cs                    # 患者 CRUD + 出院 + 同步状态
│   ├── PreferenceBus.cs                 # 偏好设置 CRUD
│   ├── ReagentBus.cs                    # 试剂 CRUD + 分组
│   ├── SampleDataBus.cs                 # 样本/质控结果查询 + HTML 标注
│   └── TestRangeBus.cs                  # 测试范围（正常/危急值）CRUD
│
├── Yzkj.Novanet.Bussiness.Bus/          # 数据同步专用业务桥接层
│   ├── NovaSyncBus.cs                   # Nova 设备数据上下行同步（核心桥接）
│   └── SyncDMSBus.cs                    # 调用存储过程同步 DMS/HIS
│
├── Yzkj.Novanet.Bussiness.Model/        # 数据传输对象（30+ DTO）
│   ├── NovaSetupModel.cs                # Nova 配置完整 DTO（~100 字段）
│   ├── NovaSetupKVModel.cs              # XML 序列化键值对模型
│   ├── NovaSTModel.cs                   # 同步时间戳 DTO
│   ├── SampleDataModel.cs               # 样本/质控数据 DTO
│   ├── PatientModel.cs                  # 患者 DTO
│   ├── NurseModel.cs                    # 护士 DTO
│   ├── ReagentModel.cs                  # 试剂 DTO
│   ├── DeviceModel.cs                   # 设备 DTO
│   ├── LocationModel.cs                 # 位置 DTO
│   ├── OrderFieldModel.cs               # 排序字段 DTO
│   ├── Poct1Item.cs                     # XML 序列化项
│   └── ...                              # 其余 *SetupModel / *GroupModel
│
├── Yzkj.Novanet.Data/                   # 数据访问层
│   ├── NovaDbContext.cs                 # EF DbContext（继承 IdentityDbContext）
│   ├── DbInitializer.cs                 # 数据库初始化（种子用户 admin/reader）
│   ├── DbResource.cs                    # 资源文件
│   └── Gender.cs                        # 性别枚举（Unkown/Male/Female/All）
│
├── Yzkj.Novanet.Data.Models/            # EF 实体模型
│   ├── AppUser.cs                       # Identity 用户
│   ├── Location.cs                      # 科室/医院树（聚合根，自引用）
│   ├── NovaSetup.cs                     # Nova 配置实体（~100 字段）
│   ├── SampleData.cs                    # 样本/质控结果
│   ├── Patient.cs                       # 患者
│   ├── Nurse.cs                         # 护士
│   ├── Device.cs                        # 设备
│   ├── NovaLog.cs                       # 日志
│   ├── TestRange.cs                     # 测试范围（正常/危急/技术范围）
│   ├── Preference.cs                    # 偏好设置
│   ├── DischargeClock.cs                # 出院时钟
│   ├── Diagcode.cs / DiagcodeGroup.cs   # 诊断项目 + 分组
│   ├── Reagent.cs / ReagentGroup.cs     # 试剂 + 分组
│   ├── LocationNurse.cs                 # 科室-护士 多对多关联
│   ├── LocationDiagcode.cs              # 科室-诊断 多对多关联
│   ├── LocationReagent.cs               # 科室-试剂 多对多关联
│   ├── NovaSetupGroup.cs                # 配置分组
│   └── DstTrackable.cs                  # 跟踪基类（软删除/时间戳）
│
├── Yzkj.Novanet.Data.Migrations/        # 数据库迁移历史（25+ 迁移文件）
│
├── Yzkj.Novanet.Utility/                # 通用工具类
│   ├── EF6_Extension.cs                 # IQueryable 动态排序扩展
│   ├── AgeHelper.cs                     # 年龄计算
│   ├── MD5Encrypt.cs                    # MD5 加密
│   ├── GuidHelper.cs                    # GUID 工具
│   └── NumberHelper.cs                  # 数值工具
│
├── Yzkj.Novanet.Utility.Common/
│   └── ClearLogEventInfo.cs             # 日志清理
│
├── Yzkj.Novanet.WinService/             # Windows 服务宿主（通信核心）
│   ├── Program.cs                       # Topshelf 启动入口
│   ├── NovaService.cs                   # TCP Socket 服务宿主
│   ├── NovaMessageHandler.cs            # DML 协议处理器（~1577 行，核心）
│   ├── DMLSTATE.cs                      # 状态机枚举（42 个状态）
│   ├── AutoFacConfig.cs                 # IoC 注册
│   └── LoggerWrap.cs                    # NLog 封装
│
├── NovaProtocolService.csproj           # WinService 项目文件
├── Yzkj.Novanet.Bussiness.csproj        # 业务层项目文件
├── Yzkj.Novanet.Data.csproj             # 数据层项目文件
├── Yzkj.Novanet.Utility.csproj          # 工具层项目文件
├── app.config                           # 服务配置（端口/数据库连接）
└── readme.md                            # 本文件
```

---

## 四、分层架构详解

### 4.1 整体分层

```
┌──────────────────────────────────────────────────────────┐
│               Nova Protocol Service (WinService)          │
│                  Windows Service / Topshelf               │
├──────────────────────────────────────────────────────────┤
│  Yzkj.Novanet.WinService                                 │
│  ┌─────────────┐  ┌───────────────────┐                  │
│  │  NovaService │  │ NovaMessageHandler │                  │
│  │ (TCP 监听)   │  │  (DML 协议解析)    │                  │
│  └──────┬───────┘  └────────┬──────────┘                  │
├──────────┼──────────────────┼────────────────────────────┤
│  Yzkj.Novanet.Bussiness.Bus                              │
│  ┌───────────────┐  ┌───────────────────┐                 │
│  │  NovaSyncBus   │  │  SyncDMSBus       │                 │
│  │ (设备同步桥接)  │  │  (DMS存储过程)    │                 │
│  └──────┬────────┘  └────────┬──────────┘                 │
├──────────┼───────────────────┼────────────────────────────┤
│  Yzkj.Novanet.Bussiness                                  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │
│  │DeviceBus│ │Patient │ │NurseBus│ │SetupBus│ │  ...   │  │
│  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘  │
├──────┼──────────┼──────────┼──────────┼──────────┼────────┤
│  Yzkj.Novanet.Data / Yzkj.Novanet.Data.Models            │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              NovaDbContext (EF6)                     │  │
│  │         (继承 IdentityDbContext<AppUser>)            │  │
│  └─────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────┤
│                    SQL Server                              │
└──────────────────────────────────────────────────────────┘
```

### 4.2 依赖注入

通过 Autofac 注册，见 `AutoFacConfig.cs`：

```csharp
builder.RegisterType<NovaDbContext>();
builder.RegisterType<NovaSyncBus>();
container = builder.Build();
```

`NovaMessageHandler` 构造时通过 `AutoFacConfig.container.Resolve<NovaSyncBus>()` 获取业务实例。

> 注意：当前 IoC 注册较为简单，仅注册了 `NovaDbContext` 和 `NovaSyncBus`。其他 Bus 类在 Web 层中通过 `new` 直接实例化。

### 4.3 Windows 服务启动流程

见 `Program.cs`：

1. 从 `app.config` 读取 `host`（默认自动获取本机 IP）和 `port`（默认 `57380`）
2. 通过 Topshelf 注册为 Windows 服务 `Nova Protocol Service`
3. 服务启动时调用 `NovaService.Start(host, port)` 开始监听
4. 服务支持暂停（Pause）/ 继续（Resume）/ 停止（Stop）

见 `NovaService.cs`：

- `Start()`：创建 TCP Socket，设置 KeepAlive，Bind+Listen(10)，启动后台线程 `ListenClientConnect`
- `ListenClientConnect()`：循环 `Accept()` 新连接，每个连接分配独立线程运行 `NovaMessageHandler.ReceiveMessage()`
- 每个连接的 Socket 设置：`SendBufferSize=32768`, `ReceiveBufferSize=32768`, KeepAlive
- `Pause()/Resume()`：通过 `ManualResetEvent` 挂起/恢复 Accept 循环
- `Stop()`：关闭 Socket，等待线程退出（超时 1 秒后 Abort）

---

## 五、DML 协议通信机制（核心）

### 5.1 协议概述

Nova 设备与服务器之间使用 **DML（Device Messaging Language）** 协议通信，基于 **XML 文本格式**，通过 TCP 传输。消息类型以 XML 根节点标识：

| 消息类型 | 方向 | 说明 |
|---------|------|------|
| `HEL.R01` | 设备→服务器 | Hello 握手（设备自我介绍，含序列号/型号/硬件版本等） |
| `ACK.R01` | 双向 | 确认应答（含 `ack_control_id` 用于匹配请求） |
| `DST.R01` | 设备→服务器 | 设备状态（含待同步数据量和各数据的最后更新时间戳） |
| `OBS.R01` | 设备→服务器 | 血糖结果（患者样本） |
| `OBS.R02` | 设备→服务器 | 质控结果 |
| `EVS.R01` | 设备→服务器 | 设备事件 |
| `KPA.R01` | 双向 | 心跳保活 |
| `REQ.R01` | 服务器→设备 | 请求数据（`ROBS`=请求结果/`RDEV`=请求事件） |
| `DTV.R01` | 服务器→设备 | 控制指令（`START_CONTINUOUS`=持续通信模式） |
| `DTV.R02` | 服务器→设备 | 设置时间（`SET_TIME`） |
| `EOT.R01` | 双向 | 传输结束通知（含 `topic_cd` 标识数据类型） |
| `END.R01` | 服务器→设备 | 终止会话 |
| `ESC.R01` | 服务器→设备 | 错误/异常中止 |

### 5.2 消息处理主循环

`NovaMessageHandler.ReceiveMessage()` 是所有消息的入口：

```
1. 循环接收 TCP 数据 → 缓冲区拼接 → UTF8 解码 → XML 解析
2. 根据根节点 localName 分发到对应 Handle* 方法
3. 处理完毕后调用 StatusWork() 推进状态机
4. 异常处理：XmlException（记录日志），其他异常（关闭连接）
```

### 5.3 状态机（DMLSTATE 42 个状态）

完整状态流转：

```
                    ┌─────────────────┐
                    │   WAIT_HELLO    │
                    └────────┬────────┘
                             │ HEL.R01
                             ▼
                    ┌─────────────────┐
                    │   ACK_HELLO     │
                    └────────┬────────┘
                             │ 发送 ACK + 保存设备信息
                             ▼
                    ┌─────────────────┐
                    │  WAIT_STATUS    │
                    └────────┬────────┘
                             │ DST.R01
                             ▼
                    ┌─────────────────┐
                    │  ACK_STATUS     │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
           observations > 0   observations == 0
                    │                 │
                    ▼                 ▼
           ┌─────────────┐   ┌─────────────┐
           │  REQ_OBS    │   │  OBS_EOT    │
           └──────┬──────┘   └──────┬──────┘
                  │                 │ events > 0?
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  REQ_EVS    │
                  │          └──────┬──────┘
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  EVS_EOT    │
                  │          └──────┬──────┘
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │ SET_TIME    │ ← 发送服务器时间
                  │          └──────┬──────┘
                  │                 │
                  │                 ▼
                  │          ┌─────────────────┐
                  │          │ SET_TIME_RCV_ACK │
                  │          └──────┬──────────┘
                  │                 │
                  │    ┌── Setup 需要刷新? ───┐
                  │    │ 否                       │ 是
                  │    ▼                          ▼
                  │  SETUP_EOT          SETUP_SEND_EOT
                  │    │                          │
                  │    │                  (发送 Setup 配置)
                  │    │                          │
                  │    │                  SETUP_RCV_ACK
                  │    │                          │
                  │    └──────────┬───────────────┘
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  LOC_SEND   │ (发送医院/科室列表)
                  │          └──────┬──────┘
                  │                 │
                  │          LOC_RCV_ACK
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  OPR_SEND   │ (全量/增量分页发送护士)
                  │          └──────┬──────┘
                  │                 │
                  │          OPR_RCV_ACK
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  PAT_SEND   │ (分页发送患者)
                  │          └──────┬──────┘
                  │                 │
                  │          PAT_RCV_ACK
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │  REAG_SEND  │ (分页发送试剂)
                  │          └──────┬──────┘
                  │                 │
                  │          REAG_RCV_ACK
                  │                 │
                  │                 ▼
                  │          ┌─────────────┐
                  │          │    END      │
                  │          └──────┬──────┘
                  │                 │
                  │    ┌── AutoReConnect? ──┐
                  │    │ 是                    │ 否
                  │    ▼                       ▼
                  │  CONTINUOUS          END (会话终止)
                  │  (定时重连)
                  └─────────────────────────────────────────
```

### 5.4 关键状态说明

| 状态 | 含义 |
|------|------|
| `WAIT_HELLO` → `ACK_HELLO` | 等待/确认设备握手，保存设备信息，获取同步时间戳 |
| `ACK_STATUS` | 收到设备状态上报，判断是否有新数据待同步 |
| `REQ_OBS` | 向设备请求血糖结果（发送 `ROBS` 指令） |
| `REQ_EVS` | 向设备请求设备事件（发送 `RDEV` 指令） |
| `OBS_EOT` / `EVS_EOT` | 结果/事件传输结束 |
| `SET_TIME` / `SET_TIME_RCV_ACK` | 同步服务器时间到设备 |
| `SETUP_SEND_EOT` / `SETUP_RCV_ACK` / `SETUP_EOT` | 发送完整 Nova 配置到设备 |
| `LOC_SEND_EOT` / `LOC_RCV_ACK` / `LOC_EOT` | 发送医院/科室列表到设备 |
| `OPR_SEND_EOT` / `OPR_RCV_ACK` / `OPR_EOT` | 分页发送护士数据 |
| `PAT_SEND_EOT` / `PAT_RCV_ACK` / `PAT_EOT` | 分页发送患者数据 |
| `REAG_SEND_EOT` / `REAG_RCV_ACK` / `REAG_EOT` | 分页发送试剂数据 |
| `CONTINUOUS` / `CONTINUOUS_RCV_ACK` | 进入持续通信模式（按 `CycleMinutes` 定时重连） |
| `END` | 通信正常结束 |

### 5.5 增量同步游标机制

设备在 `DST.R01` 消息中上报各数据的最后更新时间：

| 字段 | 含义 |
|------|------|
| `operators_update_dttm` | 护士数据最后更新时间 |
| `patients_update_dttm` | 患者数据最后更新时间 |
| `setup_update_dttm` | 配置最后更新时间 |
| `loc_list_update_dttm` | 科室列表最后更新时间 |
| `phys_update_dttm` | 医师数据最后更新时间 |
| `reag_update_dttm` | 试剂数据最后更新时间 |

服务器侧通过 `Location` 实体的 `ST_*` 字段存储各数据的最后同步时间戳（`ST_Location`, `ST_Setup`, `ST_Nurse`, `ST_Patient`, `ST_Reagent`）。

`NovaMessageHandler` 中的 `SetupRefresh` / `LocationRefresh` / `NurseRefresh` / `PatientRefresh` / `ReagentRefresh` 属性比对两侧时间戳，决定是否需要重新下发：

```csharp
public bool SetupRefresh
{
    get
    {
        if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00") return true;  // 设备从未同步过
        if (NSTModel == null || !NSTModel.ST_Setup.HasValue) return false;
        DateTime deviceTime = DML2DateTime(setup_update_dttm);
        return NSTModel.ST_Setup.Value > deviceTime;  // 服务器比设备新
    }
}
```

> 特殊值 `"2000-01-01T00:00:00.00+08:00"` 表示设备从未同步过该类数据，此时强制触发全量下发。

### 5.6 分页传输机制

大量数据（护士/患者/试剂）采用分页传输：

- 每页最大条数：`MAXCOUNT = 20`（试剂为 50）
- 同时受 `max_message_sz` 限制（设备在 `HEL.R01` 中上报），单条消息 XML 大小不超过 `max_message_sz - 8192 - 32`
- 通过 `pi`（page index，即最后发送记录的 ID）追踪已发送位置
- `page_over` 标记当前数据类型是否全部发送完毕
- 每发完一页后记录 `pi = 最大ID`，下次从该位置继续

护士数据支持两种模式：
- `SendOperatorList()`（OPL.R01）：全量下发
- `SendOperator2List()`（OPL.R02）：增量下发，包含删除标记（`action_cd=D`）和新增标记（`action_cd=I`）

---

## 六、数据模型关系

### 6.1 核心实体关系（ER 图）

```
┌──────────────┐       1:N       ┌──────────────┐
│   Location   │───────────────►│   Patient    │
│  (科室/医院)  │                │   (患者)     │
└──────┬───────┘                └──────────────┘
       │
       │ 1:1
       ├──────────────────────────┐
       │                          │
┌──────┴───────┐          ┌──────┴───────┐
│   Preference  │          │  TestRange   │
│  (偏好设置)   │          │ (测试范围)   │
└──────────────┘          └──────────────┘
       │                          │
       │                          │
       │                  ┌──────┴───────┐
       │                  │ NovaSetup    │
       │                  │ (设备配置)   │
       │                  └──────────────┘
       │                          │
       │                          │ 1:N
       │                          ├─────────────────────────┐
       │                          ▼                         ▼
       │                  ┌──────────────┐          ┌──────────────┐
       │                  │ LocationNurse │          │LocationDiag  │
       │                  │ (科室-护士)   │          │(科室-诊断)   │
       │                  └──────┬───────┘          └──────┬───────┘
       │                         │                         │
       │                  ┌──────┴───────┐          ┌──────┴───────┐
       │                  │    Nurse     │          │  Diagcode    │
       │                  │  (护士)      │          │ (诊断项目)   │
       │                  └──────────────┘          └──────┬───────┘
       │                                                    │
       │                  ┌──────────────┐          ┌──────┴───────┐
       │                  │LocationReagent│          │DiagcodeGroup │
       │                  │ (科室-试剂)   │          │ (诊断分组)   │
       │                  └──────┬───────┘          └──────────────┘
       │                         │
       │                  ┌──────┴───────┐          ┌──────────────┐
       │                  │   Reagent    │◄─────────│ ReagentGroup │
       │                  │   (试剂)     │          │ (试剂分组)   │
       │                  └──────────────┘          └──────────────┘
       │
       │ 1:N
       ▼
┌──────────────┐       1:N        ┌──────────────┐
│DischargeClock│──────────────►│  NovaLog    │
│ (出院时钟)   │                │  (系统日志)  │
└──────────────┘                └──────────────┘
```

### 6.2 Location（科室/医院树）— 聚合根

`Location` 是整个系统的聚合根，采用**自引用树形结构**：

- `Level = 0`：医院（根节点，`ParentId = null`）
- `Level = 1`：科室（叶子节点，`ParentId` 指向医院）
- 软删除：`IsDeleted` + `DeleteTime`
- 包含多个时间戳字段用于增量同步判断：
  - `ST_Location` / `ST_Setup` / `ST_Nurse` / `ST_Patient` / `ST_Reagent`

### 6.3 NovaSetup（设备配置）

`NovaSetup` 包含约 **100 个字段**，全部为 `string` 类型，按功能分组：

| 字段前缀 | 功能域 | 说明 |
|---------|--------|------|
| `AccnId*` | 就诊号(Accession) | 扫码格式、长度、验证策略、2D 码支持 |
| `DxId*` | 诊断ID | 诊断号扫码策略 |
| `PatId*` | 患者ID | 患者标识方式（PATID/MRN/ACCT） |
| `PhysId*` | 医师ID | 医师扫码策略 |
| `OpLogin*` | 操作员登录 | 登录扫码、权限、显示方式 |
| `LinLot*` | 线性试剂批号 | 批号扫码策略 |
| `QcLot*` | 质控批号 | 质控批号扫码策略 |
| `ProfLot*` | 能力验证批号 | 能力验证扫码策略 |
| `StripId*` | 试纸条ID | 试纸条扫码策略 |
| `DockLock*` | 仪器锁 | Dock 锁定策略 |
| `PrivLevel*` | 权限等级 | 管理员/操作员权限配置 |
| `Obs*` | 观察结果 | 结果显示、注释、拒收策略 |
| `QcObs*` | 质控结果 | 质控结果显示策略 |
| `MeterMax*` | 最大记录数 | 各类记录上限 |
| `*_EnableCd` | 启用码 | `"1"`/`"0"` 字符串 |
| `*_ScanEnableCd` | 扫码启用码 | 条码格式枚举 |

### 6.4 SampleData（样本/质控数据）

核心字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `Id` | long (PK, Identity) | 自增主键 |
| `PatientId` | string | 患者标识 |
| `NurseCode` | string | 操作员代码 |
| `Hospital` / `Depart` | string | 医院/科室 |
| `Diagcode` | string | 诊断项目代码 |
| `Reuslt` | decimal | 结果值（**拼写错误，应为 Result**） |
| `Unit` | string | 单位（如 `mmol/L`） |
| `Interpretation` | string | 判定结果（N/H/L/HH/LL/>/<） |
| `NormalLimit` | string | 正常范围 |
| `CriticalLimit` | string | 危急范围 |
| `ObsTime` | DateTime | 观察时间 |
| `CreateTime` | DateTime | 创建时间 |
| `SerialNo` | string (Index, MaxLength=32) | 设备序列号 |
| `DeviceId` | string (Index, MaxLength=32) | 设备 ID |
| `ObsType` | int (Default=1) | 1=患者样本, 2=质控 |
| `QcLevel` | int | 质控水平（1/2/3/4） |
| `QcLot` | string | 质控批号 |
| `RgtLot` | string | 试剂批号 |
| `state` | int (Default=0) | 状态 |

### 6.5 Device（设备）

记录所有接入过的 Nova 设备信息：

| 字段 | 说明 |
|------|------|
| `SerialNo` (Index) | 设备序列号（唯一标识） |
| `DeviceId` (Index) | 设备内部 ID |
| `Name` | 设备名称 |
| `Hospital` / `Depart` | 设备所在医院/科室 |
| `LocationId` (FK) | 关联 Location |
| `LastTime` (Index) | 最后通信时间 |
| `ObservationsUpdateDttm` | 结果最后更新时间 |
| `OperatorsUpdateDttm` | 护士最后更新时间 |
| `EventsUpdateDttm` | 事件最后更新时间 |
| `PatientsUpdateDttm` | 患者最后更新时间 |
| `SetupUpdateDttm` | 配置最后更新时间 |
| `PhysUpdateDttm` | 医师最后更新时间 |
| `ReagUpdateDttm` | 试剂最后更新时间 |
| `LocListUpdateDttm` | 科室列表最后更新时间 |

### 6.6 初始化种子数据

`DbInitializer` 在数据库首次创建时：

1. 创建角色：`admin`、`reader`
2. 创建用户：
   - `admin` / `123456`（管理员，分配 admin 角色）
   - `reader` / `123456`（只读用户，分配 reader 角色）

---

## 七、业务层核心逻辑

### 7.1 通用 Bus 模式

所有业务类遵循统一模式：

```csharp
public class XxxBus
{
    private readonly NovaDbContext DbContext;

    public XxxBus(NovaDbContext dbContext)
    {
        DbContext = dbContext;   // 构造函数注入
    }
    // 同步/异步 CRUD 方法
}
```

### 7.2 通用分页查询模式

所有列表查询复用同一套"动态排序 + 分页"逻辑：

```csharp
// 1. 构建 IQueryable 源（含 join + where）
var source = from p in DbContext.Set<T>().AsNoTracking() ...

// 2. 多条件过滤
source = source.Where(e => e.Field == value);

// 3. 动态排序：List<OrderFieldModel> → "field desc,field2"
string sortStr = string.Join(",", sorts.Select(s => s.PropertyName + (s.IsDesc ? " desc" : "")));
source = source.OrderBy(sortStr);  // 使用 System.Linq.Dynamic

// 4. out total
total = source.Count();

// 5. Skip+Take 分页投影
return source.Skip(start).Take(length).Select(...).ToList();
```

### 7.3 LocationBus — 聚合根初始化

`LocationBus.AddLocation()` 在新增科室（Level=1）时级联初始化：

1. `Preference`：自动重连（false）、轮询周期（null）、患者 ID 类型（2=MRN）
2. `NovaSetup`：从 `setup_default.xml` 反序列化默认配置
3. `TestRange`：默认 SL=1, IC=0

同时 Location 本身被创建并关联到父级医院。

### 7.4 NovaSetupBus — 配置管理

`NovaSetupBus` 管理 Nova 仪器的全部配置，提供 **13+ 个分组配置方法**：

| 方法 | 前缀 | 功能 |
|------|------|------|
| `AddAcneSetup` | AccnId | 就诊号扫码 |
| `AddDiceSetup` | DxId | 诊断 ID 扫码 |
| `AddDoloSetup` | DockLock | 仪器锁 |
| `AddLileSetup` | LinLot | 线性试剂 |
| `AddLogoffSetup` | OpLogoff | 自动登出 |
| `AddOffsetsSetup` | TestRange | 偏移校准 |
| `AddOploSetup` | OpLogin | 操作员登录 |
| `AddPaieSetup` | PatId | 患者 ID 策略 |
| `AddPhieSetup` | PhysId | 医师 ID 策略 |
| `AddPrssSetup` | PrivLevel | 权限等级 |
| `AddGqclSetup` | QcLock | 质控锁定 |
| `AddQCleSetup` | QcLot | 质控批号 |
| `AddResultsSetup` | Obs | 结果策略 |
| `AddSideSetup` | ObsId | 提示文案 |
| `AddStleSetup` | StripId | 试纸条 ID |

统一写法：按 `LocationId` 查现有记录 → 不存在则新建 → 存在则更新 → `SaveTime=DateTime.Now`。

### 7.5 SampleDataBus — 结果展示逻辑

关键业务：

**结果颜色标注**（在业务层直接拼 HTML `<label>`）：
- `N` → 蓝色（正常）
- `H/HH/>` → 红色（偏高）
- `L/LL/<` → 绿色（偏低）
- 负值（-1）→ 空字符串

**质控判定**：`IsPass = "通过" + NormalLimit` 或 `"不通过" + NormalLimit`

**多表关联查询**：SampleData 关联 Diagcode、Nurse、Patient（通过 PatientId/MedicalRecord/Account 三种方式匹配患者）

### 7.6 PatientBus — 出院与同步

- `DischargePatients(ids)`：批量出院（`Status=1, DischargeDate=Now`）
- `DischargePatientsByDepart(id)`：按科室批量出院
- `GetSyncPatients`：查待同步患者（`SyncStatus=0`），关联 Location.Preference 获取 PatientID 类型
- `UpdatePatientSyncStatus(ids)`：标记已同步（`SyncStatus=1, SyncTime=Now`）
- 软删除：`IsDelete=true` + `DeleteTime=Now`

### 7.7 NurseBus — 事务保障

使用 `TransactionScope` 保证 Nurse + LocationNurse 多对多关联的原子写入：

- 新增：先创建 Nurse → 再批量添加 LocationNurse 关联
- 更新：先删除旧的 LocationNurse → 再批量添加新的关联
- 异常时 throw ex（丢失原始堆栈）

### 7.8 DeviceBus — 设备管理

- `GetDevicesByPage`：分页查询设备列表，支持动态排序
- 设备登记：按 `SerialNo + DeviceId` 判重，已存在则更新

---

## 八、设备同步桥接层

### 8.1 NovaSyncBus — 服务器↔设备数据同步

`NovaSyncBus` 是通信桥接核心，被 `NovaMessageHandler` 调用：

**上行（设备→服务器）：**

| 方法 | 功能 | 去重规则 |
|------|------|---------|
| `SaveDeviceConnect(DeviceModel)` | 登记设备连接 | 按 `SerialNo + DeviceId` 判重 |
| `AddSamples(List<SampleDataModel>)` | 批量保存样本/质控数据 | 样本：`ObsTime + SerialNo + PatientId`；质控：`ObsTime + SerialNo` |
| `SaveDeviceStatus(DeviceModel)` | 更新设备时间戳 | 按 `SerialNo` 查找，更新所有 `*UpdateDttm` 字段 |

**下行（服务器→设备）的增量查询：**

| 方法 | 功能 | 说明 |
|------|------|------|
| `GetNovaST(hosp, depart)` | 获取 5 个同步时间戳 | 返回 `NovaSTModel` |
| `GetNovaSetup(hosp, depart)` | 获取科室完整配置 | 含 TestRange 和 Diagcodes |
| `GetLocations()` | 获取医院/科室树 | 自引用结构，含 Childs |
| `GetNurses(hosp, depart, sid, ps)` | 全量护士 | 按 ID 分页 |
| `GetNurses(hosp, depart, last, sid, ps)` | 增量护士 | 按时间戳过滤变更 |
| `GetPatients(hosp, depart, sid, ps)` | 获取在院患者 | `Status==0`，按 ID 分页 |
| `GetReagents(hosp, depart, sid, ps)` | 获取有效试剂 | 过期过滤 + 类型过滤 |
| `GetPreference(hosp, depart)` | 获取偏好设置 | 含 AutoReConnect, CycleMinutes, PatientID |
| `ExistLocation(hosp, depart)` | 校验科室是否存在 | 用于判断设备是否已初始化 |

### 8.2 SyncDMSBus — 存储过程同步

通过 `Database.ExecuteSqlCommand` 调用 6 个存储过程与 DMS/HIS 系统同步：

| 方法 | 存储过程 | 说明 |
|------|---------|------|
| `SyncSampleData(date)` | `Proc_SyncSampleData` | 按日期同步样本数据 |
| `SyncLocation()` | `Proc_SyncLocations` | 同步科室 |
| `SyncDepts()` | `Proc_SyncDepts` | 同步部门 |
| `SyncDiags()` | `Proc_SyncDiags` | 同步诊断项目 |
| `SyncPatients()` | `Proc_SyncPatients` | 同步患者 |
| `SyncNurses()` | `Proc_SyncNurses` | 同步护士 |

> ⚠️ **安全提示**：`SyncSampleData` 使用字符串插值拼接日期参数（`$"EXEC [dbo].[Proc_SyncSampleData] '{date}'"`），存在 SQL 注入风险，应改用参数化查询。

---

## 九、数据上行处理（XML → SampleData）

### 9.1 ProcessObservation — 血糖结果（OBS.R01）

解析 `OBS.R01` 消息，XML 节点映射：

| XPath 路径 | 目标字段 | 说明 |
|-----------|---------|------|
| `SVC/SVC.observation_dttm@V` | `ObsTime` | 观察时间 |
| `PT/PT.patient_id@V` | `PatientId` | 患者标识 |
| `PT/PT.location@V` | `Hospital^Depart` | 位置（`^` 分隔） |
| `PT/OBS/OBS.value@V` + `@U` | `Reuslt` + `Unit` | 结果值 + 单位 |
| `PT/OBS/OBS.status_cd@V` | `ObsStatus` | 观察状态 |
| `PT/OBS/OBS.interpretation_cd@V` | `Interpretation` | 判定（N/H/L/HH/LL） |
| `PT/OBS/OBS.normal_lo-hi_limit@V` | `NormalLimit` | 正常范围 |
| `PT/OBS/OBS.critical_lo-hi_limit@V` | `CriticalLimit` | 危急范围 |
| `OPR/OPR.operator_id@V` | `NurseCode` | 操作员代码 |
| `RGT/RGT.lot_number@V` | `RgtLot` | 试剂批号 |
| `NTE[NTE.DIAGCODE]/firstChild.InnerText` | `Diagcode` | 诊断项目代码 |

### 9.2 ProcessObservation2 — 质控结果（OBS.R02）

解析 `OBS.R02` 消息，与 `ProcessObservation` 的主要区别：

| XPath 路径 | 目标字段 | 说明 |
|-----------|---------|------|
| `CTC/CTC.level_cd@V` | `QcLevel` | 质控水平（数字） |
| `CTC/OBS/OBS.value@V` | `Reuslt` | 质控测定值 |
| `RGT[RGT.name='TY=TS']/RGT.lot_number@V` | `RgtLot` | 试剂批号 |
| `RGT[RGT.name≠'TY=TS']/RGT.lot_number@V` | `QcLot` | 质控批号 |
| 其余字段同 ProcessObservation | | `ObsType=2` |

### 9.3 去重入库流程

`NovaSyncBus.AddSamples()` 在入库前执行去重：

```
1. 遍历每个 SampleDataModel
2. ObsType==1（患者样本）：
   去重条件 = ObsTime + SerialNo + PatientId
3. ObsType==2（质控）：
   去重条件 = ObsTime + SerialNo
4. 已存在的记录标记 Exist=true，跳过入库
5. 批量 AddRange + SaveChanges
```

---

## 十、数据下行格式化（Entity → XML）

### 10.1 SendSetup — 配置下发

```
1. 获取 NovaSetupModel → 通过 FromNovaSetup() 转 NovaSetupKVModel
2. XmlSerializer 序列化为 XML（默认命名空间）
3. 追加 TEST_CONFIG（TestRange 范围，含危急/正常区间）
4. 追加 COMMENTS（固定 "none"）
5. 追加 DIAGCODES（诊断项目列表）
6. 拼装成 <NOVA.STATSTRIP.SETUP> 消息
7. 附加 DML 头部（含 control_id, version_id=POCT1, creation_dttm）
```

### 10.2 SendPatientList — 患者下发

- 患者标识类型由 `Preference.PatientID` 决定：
  - `1` → `PATID`（患者 ID）
  - `2` → `MRN`（病历号）
  - `3` → `ACCT`（账号）
- `PT.location` 格式：`医院^科室^病区号^床号`
- 性别映射：`1→M`, `2→F`
- 分页传输，每页 MAXCOUNT=20

### 10.3 SendOperatorList / SendOperator2List — 护士下发

- `SendOperatorList()`（OPL.R01）：全量下发
- `SendOperator2List()`（OPL.R02）：增量下发
  - `UPD.action_cd=D`：删除的护士（含 OPR.operator_id）
  - `UPD.action_cd=I`：新增/变更的护士（含完整信息）
- 每个护士包含：`OPR.operator_id`、`OPR.name`、`ACC.method_cd`、`ACC.permission_level_cd`

### 10.4 SendReagents — 试剂下发

- 每条试剂包含：`LOT.lot_number`、`LOT.type`（TS=试剂/QC=质控）、`LOT.expiration_dttm`
- 质控试剂（Type=QC）额外包含：
  - `Level.number`（批号末位）
  - `Level.type=QC`
  - `TST.observation_id=Glu`
  - `TST.lo-hi_limit=[Low;High] mmol/L`

### 10.5 SendLocationList — 科室下发

- 层级结构：`<LOC><LOC.facility V="医院名"><unit V="科室名" DF="F"/></LOC.facility></LOC>`

### 10.6 消息大小限制

所有分页发送方法都检查消息大小：
```csharp
if (num >= MAXCOUNT || xmlDocument.OuterXml.Length > int.Parse(max_message_sz) - 8192 - 32)
```
- `max_message_sz`：设备上报的最大消息大小
- 预留 8192 + 32 字节用于协议开销

---

## 十一、时间处理

### 11.1 DML 时间格式

DML 协议使用带时区偏移的 ISO 格式：`2024-01-15T10:30:00.00+08:00`

### 11.2 转换方法

| 方法 | 方向 | 说明 |
|------|------|------|
| `DateTime2DML(DateTime)` | 本地 → DML | 计算 UTC 偏移，格式化为 `yyyy-MM-ddTHH:mm:ss.ff±HH:mm` |
| `DML2DateTime(string)` | DML → 本地 | 解析时区偏移，转换为本地时间 |
| `DMLDateTime(string)` | DML → DateTime | 仅取日期时间部分，忽略时区（截取前 19 字符） |

---

## 十二、日志系统

### 12.1 NLog 配置

日志类别为 `NOVA_LOGGER`，写入目标配置在 `NLog.config` 中。

### 12.2 LoggerWrap 封装

四种日志方法：

| 方法 | 用途 |
|------|------|
| `Info(userName, operatorName, userhost, data)` | 结构化日志（含设备名、操作名、主机、数据） |
| `Info(message)` | 普通信息日志 |
| `Debug(message)` | 调试信息（记录 XML 消息原文） |
| `Error(e, message)` | 错误日志（含异常堆栈） |

### 12.3 NovaLog 表

`NovaLogBus` 提供日志分页查询，支持按设备、操作类型、时间范围过滤。

---

## 十三、整体数据流

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Nova POCT 血糖分析仪                           │
│                                                                      │
│  ┌─────────────┐     TCP/XML(DML)     ┌────────────────────────────┐  │
│  │  血糖检测    │ ──────────────────→ │   Nova Protocol Service    │  │
│  │  质控检测    │                     │   (Windows Service)        │  │
│  └─────────────┘                     └────────────┬───────────────┘  │
│                                                   │ Accept()        │
│                                                   ▼                  │
│                                         ┌──────────────────────┐    │
│                                         │  NovaMessageHandler  │    │
│                                         │  (DML 协议解析/封装)  │    │
│                                         └──────────┬───────────┘    │
│                                                    │                 │
│                              ┌─────────────────────┼─────────────┐   │
│                              ▼                     ▼             ▼   │
│                     上行(写)               下行(读)              状态 │
│                   ┌─────────────┐     ┌─────────────┐      ┌─────┐ │
│                   │ NovaSyncBus │     │ NovaSyncBus │      │状态机│ │
│                   │ AddSamples │     │ Get*()      │      │DML  │ │
│                   │ SaveDevice │     │ 增量查询    │      │STATE│ │
│                   └──────┬──────┘     └──────┬──────┘      └──┬──┘ │
│                          │                    │                 │    │
│                          ▼                    ▼                     │    │
│               ┌─────────────────────────────────────────┐         │    │
│               │           NovaDbContext (EF6)            │         │    │
│               └─────────────────┬───────────────────────┘         │    │
│                                 │                                 │    │
│                                 ▼                                 │    │
│                        ┌─────────────────┐                        │    │
│                        │   SQL Server    │                        │    │
│                        └────────┬────────┘                        │    │
│                                 │                                 │    │
│              ┌──────────────────┼──────────────────┐             │    │
│              ▼                  ▼                  ▼             │    │
│     ┌─────────────────┐  ┌───────────────┐  ┌───────────────┐   │    │
│     │  SampleData    │  │   Patient     │  │    Nurse      │   │    │
│     │  (结果数据)     │  │  (患者数据)    │  │  (护士数据)    │   │    │
│     └─────────────────┘  └───────────────┘  └───────────────┘   │    │
│              │                  │                  │             │    │
│              ▼                  ▼                  ▼             │    │
│     ┌─────────────────┐  ┌───────────────┐  ┌───────────────┐   │    │
│     │  NovaSetup     │  │  Reagent      │  │  Location     │   │    │
│     │  (设备配置)     │  │  (试剂数据)    │  │  (科室数据)    │   │    │
│     └─────────────────┘  └───────────────┘  └───────────────┘   │    │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │                    SyncDMSBus                              │     │
│  │  (Proc_SyncSampleData / Proc_SyncPatients / ...)          │     │
│  └─────────────────────────┬──────────────────────────────────┘     │
│                            │                                        │
│                            ▼                                        │
│                  ┌─────────────────────┐                             │
│                  │   DMS / HIS 系统    │                             │
│                  │   (医院信息系统)     │                             │
│                  └─────────────────────┘                             │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 十四、关键配置项

### 14.1 app.config

```xml
<appSettings>
  <add key="host" value="" />        <!-- 空则自动获取本机 IP -->
  <add key="port" value="57380" />   <!-- TCP 监听端口 -->
</appSettings>
<connectionStrings>
  <add name="DefaultConnection" 
       connectionString="Data Source=...;Initial Catalog=NovanetDB;..." />
</connectionStrings>
```

### 14.2 setup_default.xml

存放于服务执行目录，作为新建科室时的默认配置模板。通过 `NovaSetupKVModel` 反序列化为 `NovaSetupModel`。

### 14.3 NovaSetup 关键字段含义

| 字段 | 含义 | 典型值 |
|------|------|--------|
| `PatIdTypeCd` | 患者标识类型 | `1`=PATID, `2`=MRN, `3`=ACCT |
| `DateFormat` | 日期格式 | `yyyy-MM-dd` |
| `TimeFormat` | 时间格式 | `HH:mm:ss` |
| `AccnIdMaxLength` / `AccnIdMinLength` | 就诊号长度限制 | 数字字符串 |
| `MeterMaxPatRec` | 患者记录上限 | 数字字符串 |
| `MeterMaxQCRec` | 质控记录上限 | 数字字符串 |
| `ObsIdMethodCd` | 结果 ID 方法 | 枚举码 |
| `SampleTypeSelectEnable` | 样本类型选择 | `1`/`0` |
| `OpLogoffModeCd` | 自动登出模式 | 枚举码 |
| `DockLockModeCd` | 仪器锁模式 | 枚举码 |
| `QcLockModeCd` | 质控锁模式 | 枚举码 |

---

## 十五、代码质量备注

| 类别 | 问题 | 位置 |
|------|------|------|
| 🔴 SQL 注入 | `SyncSampleData` 用字符串插值拼接日期参数 | `SyncDMSBus.cs#L18` |
| 🟡 拼写错误 | `Reuslt` → `Result`, `Cricital` → `Critical` | `SampleData.cs`, `NovaSetup.cs` |
| 🟡 空引用风险 | `PatientBus.UpdatePatient` 未判 null | `PatientBus.cs#L45` |
| 🟡 异常处理 | `catch { throw ex; }` 丢失堆栈 | `NurseBus.cs#L49` |
| 🟡 UI 耦合 | 业务层直接拼 HTML `<label>` | `SampleDataBus.cs#L142` |
| 🟡 N+1 查询 | 循环中逐条 `Add(LocationNurse)` | `NurseBus.cs#L37` |
| 🟡 重复代码 | 14 个 `*Bus` 重复实现排序拼接逻辑 | 各 Bus 类 |
| 🟡 Thread.Abort | 使用已过时的线程终止方式 | `NovaMessageHandler.cs`, `NovaService.cs` |
| 🟡 空值处理 | `DMLDateTime` 返回 `DateTime.MinValue` 作为失败标记 | `NovaMessageHandler.cs#L1470` |
| ⚪ 状态机复杂度 | `StatusWork()` 超长 switch（~200 行），部分状态 fall-through | `NovaMessageHandler.cs#L398` |
| ⚪ DTO 映射 | `NovaSyncBus.GetNovaSetup` 逐字段映射（~250 行） | `NovaSyncBus.cs#L154` |
| ⚪ 构造函数耦合 | `LocationBus` 构造函数需要 `NovaSetupBus` 用于初始化 | `LocationBus.cs#L17` |

---

## 十六、快速参考

### 新增科室流程
```csharp
var locationBus = new LocationBus(novaSetupBus, dbContext);
await locationBus.AddLocation("新科室", 1, parentId);
// 自动创建: Preference + NovaSetup(默认配置) + TestRange
```

### 设备接入处理流程
```
NovaService.ListenClientConnect()
  → Accept() 新连接
  → new NovaMessageHandler(socket)
  → ReceiveMessage() 循环
    → HandleHEL_R01() 保存设备信息
    → HandleDST_R01() 获取设备状态
    → StatusWork() 状态机驱动
```

### 接收设备结果流程
```
NovaMessageHandler.ReceiveMessage()
  → HandleOBS_R01(doc) / HandleOBS_R02(doc)
    → ProcessObservation() / ProcessObservation2() 解析 XML
      → 映射到 SampleDataModel
    → NovaSyncBus.AddSamples() 去重入库
    → SendAcknowledgeMessage() 回复 ACK
  → StatusWork() 推进状态机
```

### 同步配置到设备流程
```
StatusWork() 状态机驱动:
  SET_TIME → SETUP_SEND_EOT → SETUP_RCV_ACK
  → SETUP_EOT → LOC_SEND_EOT → LOC_RCV_ACK → LOC_EOT
  → OPR_SEND_EOT → OPR_RCV_ACK → OPR_EOT
  → PAT_SEND_EOT → PAT_RCV_ACK → PAT_EOT
  → REAG_SEND_EOT → REAG_RCV_ACK → REAG_EOT
  → END / CONTINUOUS

每步都走: Get*() 查询 → 格式化 XML → SendMessage() → 等 ACK
```

### 数据库迁移
```
Migration 历史: 25+ 迁移文件
初始创建: InitialCreate.cs
最新: update_sampledatas_add_qclevel.cs
```

### DML 消息速查表
```
HEL.R01    → 设备握手（含序列号、型号、版本）
DST.R01    → 设备状态（含待同步数量、时间戳）
OBS.R01    → 血糖结果（患者样本）
OBS.R02    → 质控结果
EVS.R01    → 设备事件
KPA.R01    → 心跳保活
REQ.R01    → 请求数据（ROBS/RDEV）
DTV.R01    → START_CONTINUOUS
DTV.R02    → SET_TIME
EOT.R01    → 传输结束
END.R01    → 终止会话
ACK.R01    → 确认应答
ESC.R01    → 错误中止
```
