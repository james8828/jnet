# NovaNet RTM 服务（反编译源码）功能说明

> 本文档基于 `e:\doc\jnet-nova\decompile\novanet` 目录下四个反编译 C# 服务的源码分析生成。
> 这四个服务均为 **Windows Service**（继承 `System.ServiceProcess.ServiceBase`），运行于 `.NET Framework 4.5.2`，共同组成 Nova Biomedical NovaNet 实时通信中间件。

## 目录

- [1. 概述](#1-概述)
- [2. RTMADTP — ADT 消息发布服务](#2-rtmadtp--adt-消息发布服务)
- [3. RTMADTQ — ADT 消息队列消费服务](#3-rtmadtq--adt-消息队列消费服务)
- [4. RTMLIS — 实验室信息系统接口服务](#4-rtmlis--实验室信息系统接口服务)
- [5. RTMOPL — 操作员列表管理服务](#5-rtmopl--操作员列表管理服务)
- [6. 服务间协作关系](#6-服务间协作关系)
- [7. 数据库表依赖汇总](#7-数据库表依赖汇总)

---

## 1. 概述

NovaNet 通过一组以 `RTM`（Real-Time）为前缀的 Windows 服务实现医院信息系统（HIS）、实验室信息系统（LIS）与 Nova 诊断设备之间的实时数据交换。

| 服务 | 全称（推断） | 主要职责 | 通信协议 |
|------|-------------|---------|---------|
| **RTMADTP** | Real-Time ADT Publisher | 接收 HIS 的 HL7 ADT 消息，落库并发布到 MSMQ | HL7 over MLLP |
| **RTMADTQ** | Real-Time ADT Queue | 从 MSMQ 消费 ADT 消息，推送给订阅端 | MSMQ + TCP Socket |
| **RTMLIS** | Real-Time LIS | 对接 LIS/设备，处理查询与结果上传 | DML（XML）/ ASTM / HL7 |
| **RTMOPL** | Real-Time Operator List | 管理设备操作员及权限的实时同步 | DML（XML） over TCP |

### 通用技术特征

- 均通过 `NNBase.NNBaseOpen(...)` 完成日志、数据库、注册表初始化（见各服务入口 `RTM*.cs` 构造函数）。
- 均使用 ODBC（`System.Data.Odbc`）连接 SQL Anywhere 数据库，通过 `OpenDBConnection` 获取连接。
- 均实现 `OnTimedEvent` 每分钟定时器，执行健康检查（`UpdateHealthPing`）、端口初始化、日志切换。
- 均通过 `m_NNBase.CheckIfAuthorized` / `CheckAssemblyVersion` 做授权和版本校验。
- 均支持 Apache / IIS 本地事件通知器（`event_notifier.php`）。

---

## 2. RTMADTP — ADT 消息发布服务

**源码目录**：[RTMADTP](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/RTMADTP)

### 2.1 定位

接收来自 HIS（医院信息系统）的 HL7 ADT（Admit/Discharge/Transfer）消息，解析后写入 NovaNet 数据库，并将事件发布到 MSMQ 队列 `.\private$\adt_request_queue`，供 RTMADTQ 消费。

### 2.2 入口与配置

- 服务入口：[RTMADTP.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/RTMADTP/RTMADTP.cs)
  - 构造函数中调用 `m_NNBase.NNBaseOpen(bLogging: true, "RTMADTP", "RTMADTP", "ADTP")`。
- 配置文件：[app.config](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/app.config)
  - `queueName = .\private$\adt_request_queue`：MSMQ 队列名。

### 2.3 核心组件

#### ADTConfiguration — ADT 配置
[ADTConfiguration.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/RTMADTP/ADTConfiguration.cs)

定义 ADT 接口的可配置项：
- `sAccountSegment` / `iAccountField` / `iAccountComponent`：账号号在 HL7 段中的位置。
- `sDischargeOutPatientClasses` / `sDischargeOutPatientTypes`：门诊出院类别。
- `sAdmitOnUpdateTypes`：更新类消息中触发入院的事件类型。
- `sSupportedTransactions`：支持的 ADT 事务类型（A02/A03/A04/A08 等）。
- `bMultipleVisitsPerAccount` / `bVisitNumsCrossPatients`：就诊号与账号的多重关系策略。
- `m_ActiveHours`：消息活跃时间窗。

#### HL7Protocol — HL7 协议处理
[HL7Protocol.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/RTMADTP/HL7Protocol.cs)

继承自 `Protocol`（抽象基类要求实现 `ProcessMessage()` 与 `ProcessNotify(int)`）。

**HL7 帧控制字符**：
- `ASCII_VT (\v)`：消息起始
- `ASCII_FS (\u001c)`：消息结束
- `ASCII_CR (\r)`：段分隔

**解析的 HL7 段与字段**（见字段定义 L100-L260）：
- **MSH**：`SendingApplication`、`SendingFacility`、`ReceivingApplication`、`ReceivingFacility`、`MSHTimeStamp`、`MessageType`、`MessageSubType`、`MessageControlID`、`ProcessingID`。
- **PID**：`PatientID`、`MedicalRecordNumber`（含 `MRNAssigningAuthority`/`MRNAssigningFacility`）、`AccountNumber`、`FirstName`/`LastName`/`MiddleName`/`Prefix`/`Suffix`、`BirthDate`、`Sex`、`Race_HL7`（并映射到 `Race_DML`）。
- **PV1**：`PatientClass`、`Location_PV1_3_1`、`Room`、`Bed`、多个 `Facility_PV1_*_*`（位置 3/6/11/19/39/42/43）、`AttendingPhysician`、`ReportingPhysician`、`ConsultingPhysician`、`PatientType`、`VisitNumFromADT`、`VisitUUID`。
- **PV1 体重/身高**：`Weight_HL7_value/units`（并转换到 `Weight_DML_*`）、`Height_HL7_value/units`。

**支持的 ADT 事件类型**（基于字段语义）：
- `A02`（转科 Transfer）、`A03`（出院 Discharge）、`A04`（入院 Admit）、`A08`（更新 Update）等。
- 维护 `Previous*` 系列字段，用于在 A08/A02 等更新事件中对比前后就诊/账号/设施变化。

**MSMQ 集成**（`MQMessageAPI` 内部类，L21-L28）：
- 通过 P/Invoke 调用 `mqrt.dll`：
  - `MQOpenQueue`：打开 MSMQ 队列。
  - `MQMoveMessage`：在队列间移动消息。
- 将解析后的 ADT 事件写入 `.\private$\adt_request_queue`。

### 2.4 主要数据流

```
HIS  ──HL7/MLLP──▶  RTMADTP
                       │
                       ├─▶ 解析 HL7（MSH/EVN/PID/PV1）
                       ├─▶ 写入 DBA.patients / DBA.patient_visits
                       └─▶ 发布到 MSMQ: .\private$\adt_request_queue
```

---

## 3. RTMADTQ — ADT 消息队列消费服务

**源码目录**：[RTMADTQ](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ)

### 3.1 定位

从 MSMQ 队列 `.\private$\adt_request_queue` 消费 ADT 消息，作为 TCP 客户端/服务端将 ADT 事件推送给下游订阅端（如 RTMLIS 或设备网关）。

### 3.2 入口与配置

- 服务入口：[RTMADTQ.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/RTMADTQ.cs)
  - 构造函数中 `NNBaseOpen(..., "RTMADTQ", "RTMADTQ", "ADTQ")`。
  - 从注册表读取 `BinDir`（默认 `C:\NovaBiomedical\NovaNet\Bin`），用于定位 `RTMADT.xml` 配置。
- 配置文件：[app.config](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/app.config)
  - `queueName = .\private$\adt_request_queue`。
  - WCF 客户端端点：`MessageResponseEndpoint`，绑定 `msmqIntegrationBinding`，TTL 7 天，`security mode="None"`。

### 3.3 核心组件

#### IMessageProcessor / MessageProcessorClient — WCF MSMQ 客户端
- [IMessageProcessor.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/IMessageProcessor.cs)：定义 `SubmitStringMessage(MsmqMessage<string>)` 单向操作契约。
- [MessageProcessorClient.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/MessageProcessorClient.cs)：继承 `ClientBase<IMessageProcessor>`，通过 WCF 通道向 MSMQ 提交字符串消息。

#### Port — 端口抽象
[Port.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/Port.cs)

- 抽象基类 `CommType`：定义 `Run()` / `Notify(int)` / `IsAlive()` / `IsConnected()`。
- `SocketServerCommType`：封装 `AsynchNetworkServer`，作为 TCP 服务端监听。
- `SocketClientCommType`：封装 `AsynchNetworkClient`，作为 TCP 客户端主动连接。
- 支持服务端/客户端两种模式，由 `RTMADT.xml` 配置决定。

#### TalkSocket — Socket 保活
[TalkSocket.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/TalkSocket.cs)

- 通过 `SIO_KEEPALIVE_VALS` IOControl 设置 TCP Keep-Alive 参数（`NormFreq` 频率、`TimeoutRetryTime` 重试）。
- 配置 `LingerOption` 控制关闭时的延迟发送。

### 3.4 运行机制

- `OnStart` 启动 1 秒定时器，首次触发后调整为 60 秒周期（[RTMADTQ.cs L233-L280](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTQ/RTMADTQ/RTMADTQ.cs#L233-L280)）。
- `OnTimedEvent` 执行：
  1. `InitializePorts()`：读取 `RTMADT.xml` 配置，清理失效端口，创建新端口。
  2. `GetDBDataForService()`：获取数据库版本、授权校验、读取历史消息计数 `m_iTotMessages`。
  3. `UpdateHealthPing()`：心跳上报 `dba.health_ping`，监控工作线程存活（线程死亡则 `ShutDown`）。
- `ConnectedToADTFeeder` 属性标识是否已与 ADT 数据源建立连接。

### 3.5 主要数据流

```
MSMQ: .\private$\adt_request_queue  ──▶  RTMADTQ
                                            │
                                            ├─▶ WCF msmqIntegrationBinding 消费
                                            └─▶ TCP Socket (Server/Client) 推送
                                                  │
                                                  ▼
                                            下游订阅端（RTMLIS / 设备网关）
```

---

## 4. RTMLIS — 实验室信息系统接口服务

**源码目录**：[RTMLIS](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS)

### 4.1 定位

面向 LIS（实验室信息系统）与诊断设备的网关服务，**同时支持三种协议**：DML（XML）、ASTM（E1394）、HL7，实现查询应答、结果上传、订单下发。

### 4.2 入口与配置

- 服务入口：[RTMLIS.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS/RTMLIS.cs)
  - `NNBaseOpen(..., "RTMLIS", "RTMLIS", "LIS")`。
  - 使用 `CompareInfo("en-US")` + `CompareOptions.IgnoreCase` 做不区分大小写比较。
  - 探测数据库列：`m_bSamplesDeviceNameColumn`、`m_bInstrumentsTestsLisTestAliasColumn`、`m_bPatientVisitsTable`。
- 配置文件：[app.config](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/app.config)（无 MSMQ 配置）。

### 4.3 核心组件

#### DMLProtocol — DML 协议处理
[DMLProtocol.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS/DMLProtocol.cs)

- 与 ICPMGR 中的 DML 协议同源（XML 格式），但面向 LIS 场景。
- 常量：`DML_MAXTOPICSSUPPORTED = 16`、`DML_MAXDIRECTIVESSUPPORTED = 16`。
- 消息缓冲：`m_inbuffer[32768]`、`m_outbuffer`。
- 维护 `m_Topics[16]` / `m_Directives[16]` 支持的主题与指令表。
- `m_imsgid`（初始 4000）+ `MsgId[10]` + `m_LastMessageSent[10]`：消息 ID 与重发缓存。
- `m_sample_key_num` / `m_SelectSampleKeyNum`：样本键管理。
- `m_ProtocolThread`：独立协议处理线程。
- XML 帧解析：通过 `m_angleCount`（尖括号计数）与 `m_chLast` 判定 XML 消息边界。

#### ASTMProtocol — ASTM E1394 协议处理
[ASTMProtocol.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS/ASTMProtocol.cs)

- 实现 ASTM E1394 标准的帧协议。
- 帧字段：`m_nCurFrameSend` / `m_nCurFrame`（当前帧号）、`m_FrameChecksum` / `m_szChecksum[3]`（校验和）、`m_bLastFrame`（是否末帧）。
- 解析设备/样本字段：`m_device_id`、`m_serial_id`、`m_inst_type`、`m_inst_name`、`m_inst_ver`、`m_control_type`、`sampleDateTime`、`operator_id`、`releaser_id`、`strip_lot_num`、`order_id`、患者 ID 三件套（`enterprise_id`/`medrec_num`/`account_num`）。

#### ASTM_STATE — ASTM 状态机
[ASTM_STATE.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS/ASTM_STATE.cs)

定义 ASTM 协议的读写状态：

| 状态 | 含义 |
|------|------|
| `ASTM_READ_IDLE` | 读空闲 |
| `ASTM_READ_WAITING_FOR_STX` | 等待帧起始 STX |
| `ASTM_READ_WAITING_FOR_FN` | 等待帧号 |
| `ASTM_READ_WAITING_FOR_ETB_OR_ETX` | 等待帧中结束(ETB)或帧尾(ETX) |
| `ASTM_READ_WAITING_FOR_CHECKSUM_MSB` | 等待校验和高字节 |
| `ASTM_READ_WAITING_FOR_CHECKSUM_LSB` | 等待校验和低字节 |
| `ASTM_READ_WAITING_FOR_CR` / `ASTM_READ_WAITING_FOR_LF` | 等待 CR / LF |
| `ASTM_WRITE_*` | 写状态族（待发/发包/等 ACK） |

#### State — 会话状态
[State.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMLIS/RTMLIS/State.cs)

保存单次会话的运行时状态：`queryMessage` / `resultMessage`（ArrayList）、`recordList`、`queryTestList`、`bFullMessage`、`bSendingMessage`、`bWaitingForQueryResponse`、`bRetryLastQueryMessage` / `bRetryLastResultMessage`。

### 4.4 主要数据流

```
设备/LIS  ──DML(XML)/ASTM/HL7──▶  RTMLIS
                                   │
                                   ├─▶ 处理 QUERY.R01 → 返回订单/患者
                                   ├─▶ 处理 OBS.R01   → 结果落库
                                   ├─▶ 下发订单 (Order)
                                   └─▶ 状态机驱动 ASTM 帧收发
```

---

## 5. RTMOPL — 操作员列表管理服务

**源码目录**：[RTMOPL](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL)

### 5.1 定位

基于 DML 协议管理设备操作员（Operator）及其权限、方法、位置映射的实时同步。接收设备上报的操作员变更并写入数据库，或将数据库中的操作员列表下发给设备。

### 5.2 入口与配置

- 服务入口：[RTMOPL.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/RTMOPL.cs)
  - `NNBaseOpen(..., "RTMOPL", "RTMOPL", "OPL")`。
  - `MaxReadBuffSize = 32768`，`m_OPLBytesBuffers`（共享缓冲）。
  - 探测列：`m_bTestNameColumn`、`m_bMethodsTable`、`m_b_loc_last_update_inst_class_column`、`m_b_loc_last_update_inst_type_column`。
- 配置文件：[app.config](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/app.config)
  - `UseFacilityUUID = F`：不使用设施 UUID。

### 5.3 核心组件

#### DMLProtocol — DML 协议处理
[DMLProtocol.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/DMLProtocol.cs)

- 同样以 DML（XML）协议通信，`DML_MAXTOPICSSUPPORTED = 16`。
- 操作员相关字段：`m_OperatorID`、`m_OperatorLastName`、`m_OperatorFirstName`、`m_active_date`、`m_expiration_date`、`m_permission_level`、`m_password`、`m_action_cd`、`m_operator_num`。
- 设备/位置上下文：`m_facility`、`m_location`、`m_loc_num`、`m_insttype`、`m_inst_class`、`m_method`。
- 列表缓存：`m_method_list`、`m_unused_method_list`、`m_instrument_list`、`m_location_list`。
- `m_OplistMsg`：待下发的操作员列表消息。

#### 数据记录类

| 类 | 文件 | 对应表 | 说明 |
|----|------|--------|------|
| `OperatorRec` | [OperatorRec.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/OperatorRec.cs) | `dba.operators` + `dba.contact_info` | 操作员主记录：`OperatorNum`、`SupervisorNum`、`OperatorID`、`IsSupervisor`、`Lastname`/`Firstname`/`Initials`/`email` |
| `OperatorPrivilegeRec` | [OperatorPrivilegeRec.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/OperatorPrivilegeRec.cs) | `dba.operator_privileges` | 权限：`insttype`、`pswd`、`certstartdate`/`certenddate`、`privilege`、`isactive`、`testname` |
| `OperatorToUnitRec` | [OperatorToUnitRec.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/OperatorToUnitRec.cs) | `dba.operator_to_unit` | 操作员-位置映射：`OperatorNum`、`locnum`、`isactive` |
| `MethodRec` | [MethodRec.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/MethodRec.cs) | `dba.methods` | 操作员-仪器-方法：`OperatorNum`、`insttype`、`methodcd` |

#### 数据访问类

- [DBOperator.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/DBOperator.cs)
  - `CreateOperator`：INSERT `dba.operators`（`Operator_num` = GUID，`Supervisor_num`、`Operator_ID`、`Is_Supervisor`、`last_update_date`、`add_date`）。
  - `CreateContactInfo`：INSERT `dba.contact_info`（`ref_table='OPERATORS'`，`contact_num`，`Last_Name`/`First_Name`/`Initials`/`Email`）。
- [DBMethod.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/DBMethod.cs)
  - `Create`：先 `Read` 查重，不存在则 INSERT `dba.methods`（`operator_num`、`inst_type`、`method_cd`）。
- [DBOperatorPrivilege.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/DBOperatorPrivilege.cs)：操作员权限 CRUD。
- [DBOperatorToUnit.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMOPL/RTMOPL/DBOperatorToUnit.cs)：操作员-位置映射 CRUD。

所有 DB 类均统一通过 `DBRecStatus` 记录错误（`m_bOK`、`m_errortype`、`m_while`、`m_SA_e`/`m_e`、`m_SQL`），并由 `m_NNBase.LogActionAndError` 落审计日志。

### 5.4 主要数据流

```
设备  ──DML(XML)──▶  RTMOPL
                      │
                      ├─▶ 接收操作员变更 → CreateOperator/ContactInfo/Method/Privilege/ToUnit
                      ├─▶ 下发操作员列表 (m_OplistMsg)
                      └─▶ 按 inst_type / loc_num 过滤权限与方法
```

---

## 6. 服务间协作关系

```
                ┌─────────────┐
   HIS ─────────▶│  RTMADTP    │──MSMQ──▶ .\private$\adt_request_queue
   (HL7 ADT)    │  (Publisher)│
                └─────────────┘
                                        │
                                        ▼
                ┌─────────────┐   ┌─────────────┐
                │  RTMADTQ    │◀──│   MSMQ      │
                │  (Consumer) │   │  adt_queue  │
                └──────┬──────┘   └─────────────┘
                       │ TCP Socket
                       ▼
                ┌─────────────┐
                │  RTMLIS     │◀──── 设备/LIS (DML/ASTM/HL7)
                │  (LIS 网关) │
                └─────────────┘

                ┌─────────────┐
   设备 ────────▶│  RTMOPL     │ (DML)
   (操作员同步)  │  (Operator) │
                └─────────────┘
```

- **ADT 链路**：HIS → RTMADTP（落库 + 入队）→ MSMQ → RTMADTQ（出队 + 推送）→ 下游。
- **LIS 链路**：RTMLIS 直连设备/LIS，独立完成查询应答与结果上传。
- **操作员链路**：RTMOPL 直连设备，独立完成操作员/权限同步。
- **ICPMGR**（另见 `decompile/novanet/ICPMGR`）：与 RTMLIS 同属 DML 协议族，是设备侧的主协议管理器，RTMLIS/RTMOPL 可视为其在 LIS 与操作员域的特化。

---

## 7. 数据库表依赖汇总

| 服务 | 读/写 | 涉及表 |
|------|-------|--------|
| RTMADTP | 写 | `dba.patients`、`dba.patient_visits`、`dba.communications`、`dba.health_ping`、`dba.version_info` |
| RTMADTQ | 读/写 | `dba.health_ping`、`dba.version_info`、ADT 订阅配置（`RTMADT.xml`） |
| RTMLIS | 读/写 | `dba.samples`、`dba.instruments_tests`、`dba.patient_visits`、`dba.orders`、`dba.patients`、`dba.communications` |
| RTMOPL | 读/写 | `dba.operators`、`dba.contact_info`、`dba.methods`、`dba.operator_privileges`、`dba.operator_to_unit`、`dba.loc_last_update`、`dba.inst_locations` |
| 全部 | 读 | `dba.version_info`（版本）、授权校验表 |

---

> **说明**：以上分析基于反编译源码的结构与字段语义推断。各方法的完整实现细节（如 `ProcessMessage` 的完整分支）需结合具体方法体进一步研读。文档中行号引用以反编译目录下的原始文件为准。
