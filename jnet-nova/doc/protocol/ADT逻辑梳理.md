

# HL7Protocol.cs 类功能详解

## 一、类的整体定位

[HL7Protocol.cs](file:///e:/doc/jnet-nova/decompile/novanet/RTMADTP/RTMADTP/HL7Protocol.cs) 是 Nova Biomedical 公司 NovaNet 系统中 **RTMADTP（Real-Time ADT Processor，实时 HL7 ADT 消息处理器）** 的核心类，继承自 `Protocol` 基类，命名空间为 `RTMADTP`。

它本质上是一个 **HL7 v2.x ADT（Admit/Discharge/Transfer）消息消费者**：从 Microsoft Message Queue (MSMQ) 读取 HIS（医院信息系统）推送的 HL7 消息，解析后写入 NovaNet 数据库（Sybase），并维护患者就诊轨迹，供后续设备（如生化分析仪）查询使用。

---

## 二、类的内部结构

### 1. 嵌套 P/Invoke 类 `MQMessageAPI`
通过 `mqrt.dll` 调用 MSMQ 原生 API，提供：
- `MQOpenQueue`：打开队列
- `MQMoveMessage`：将消息移动到子队列（如 reject_queue）

### 2. ASCII 控制字符常量（HL7 协议要求）
```
ASCII_VT  = '\v'       (0x0B)  HL7 消息起始符
ASCII_FS  = '\u001c'   (0x1C)  HL7 消息结束符
ASCII_CR  = '\r'       (0x0D)  段分隔符
ASCII_STX = '\u0002'   (0x02)
ASCII_ETX = '\u0003'   (0x03)
```

### 3. 大量状态字段
类中维护近百个 `bool` 标志位和字符串字段，用于：
- **消息元数据**：SendingApplication、MessageType、MessageSubType、MessageControlID、ProcessingID
- **患者标识**：PatientID、MedicalRecordNumber、AccountNumber、VisitNum
- **患者人口学**：FirstName、LastName、Sex、BirthDate、Race
- **就诊信息**：PatientClass、Location、Room、Bed、AttendingPhysician
- **前一次标识**（用于合并/移动场景）：PreviousPatientID、PreviousMedicalRecordNumber、PreviousPatientAccount、PreviousVisitNum
- **跨设施场景标志**：bCrossFacilityTransfer、bCrossFacilityMergePatient、bCrossFacilityMoveAccount 等
- **数据库连接**：myDBReadConnection / myDBWriteConnection / myPTDBWriteConnection（三个独立的 ODBC 连接，最后一个用于 Patient_Track 库）

---

## 三、构造函数：初始化流程（第 690–786 行）

构造函数 `HL7Protocol(bool logging, RTMADTP parent)` 完成全部启动准备：

1. **打开 NNBase 工具类**：日志、审计、错误上报基础设施
2. **打开三套数据库连接**：
   - `myDBReadConnection`：只读 NovaNet 主库
   - `myDBWriteConnection`：写入 NovaNet 主库
   - `myPTDBWriteConnection`：写入 Profile_Track（患者跟踪）库，连接串 `DSN=...;UID=...;PWD=...`
3. **心跳登记**：执行 `update DBA.health_ping set update_time=now(*) where process_name='RTMADTP'`
4. **加载机构列表 `FacilityList`**：若为空则强制日志记录 "NoFacilitiesInList"；若仅 1 个则置 `bSingleFacility=true`
5. **加载位置列表 `LocationList`**：科室/床位映射
6. **加载配置文件 `LoadConfigFile`**：从注册表 `BinDir` 与数据库端口表读取
7. **加载自动出院表 `LoadAutoDischargeTables`**：门诊类别/类型自动出院规则
8. **初始化患者查询对象**：
   - `m_PatientQuery` / `m_PrevIDs_PatientQuery`：当前/历史患者查询
   - `m_DBPatient` / `m_newDBPatient` / `m_PrevIDs_DBPatient`：DB 患者记录对象
   - `m_PatientVisitRec` / `m_PatientAccountRec`：就诊/账户记录
9. **启动后台线程**：`m_ProtocolThread = new Thread(ProtocolThread)`

---

## 四、消息接收：MSMQ 异步驱动

### 1. `ProtocolThread()` (第 6870 行)
线程入口，调用 `InitializeMSMQ()` 后进入空循环（每秒 sleep），仅用于保持进程存活——真正的消息驱动靠回调。

### 2. `InitializeMSMQ()` (第 6889 行)
- 若队列不存在则创建事务性队列 `MessageQueue.Create(..., transactional: true)`
- 设置 Administrators 完全控制权限
- 调用 `CreateRejectQueue` 打开 `reject_queue` 子队列句柄
- 注册 `PeekCompleted` 事件 → `GetMSMQMessage`，调用 `BeginPeek()` 启动异步监听

### 3. `GetMSMQMessage()` (第 6928 行) — 消息入口回调
- 校验消息 Label 包含 `RTMADTQ`（防止误投），否则直接删除
- 将 Body 反序列化为字符串赋给 `m_message`
- 调用核心方法 `ProcessMessage()`
- 异常时调用 `ShutDown(..., bExit: true)` 关闭整个处理器

---

## 五、消息处理主流程

### 1. `ProcessMessage()` (第 1668 行) — 段解析与拆分
**两遍扫描**策略：

- **第一遍 (iPass=0)**：解析 MSH 段获取消息子类型（如 A01、A08、A17），调用 `MessageSubTypeSupported` 校验是否支持（默认支持 A01-A62 中约 40 种）；同时统计 PID/MRG 段数量，**若超过 1 个则置 `bSplitMessage=true`**（如 A17 床位互换、A18/A40 患者合并、A47/A44/A45 等"多患者"消息）
- **第二遍 (iPass=1)**：将消息按 PID/MRG 切分为多个子消息存入 `SubMessage` ArrayList，或对单患者消息直接处理各段：
  - `MSH` → `ProcessMessageHeaderSegment`
  - `EVN` → `ProcessEventSegment`
  - `PID` → `ProcessPatientIdentificationSegment`
  - `PV1` → `ProcessPatientVisitSegment`
  - `OBX` → `ProcessResultSegment`（体重、身高等观察值）
  - `DG1` → `ProcessDiagnosisSegment`
  - `MRG` → `ProcessMergeSegment`（合并/移动源患者信息）

处理完成后调用 `RemoveMessageFromQueue()` 移除消息，并递增 `m_parent.m_iNumMessages`。

### 2. `ProcessParsedMessage()` (第 1872 行) — 单消息主入口
1. `myPatientTrackingRec.Begin(...)`：在 Profile_Track 表登记消息开始处理
2. 调用 `ADTMessageOK("", "")` 进行消息合法性校验
3. **跨设施分支** (`bSpansFacilities=true`)：用 `m_PatientQuery.GetPatientFacilityList` 查询该患者所在的全部设施，对每个设施循环调用 `ProcessMessageForFacility`
4. **单设施分支**：依次调用 `OKToAddOrUpdatePatient()` → `ProcessADTMessage()`
5. `myPatientTrackingRec.Commit(...)`：提交跟踪记录

### 3. `ProcessMessageForFacility()` (第 2345 行)
跨设施场景下，每个设施的子处理入口，结构同上但传入设施参数。

---

## 六、HL7 段解析（第 6229–6417 行）

| 方法 | 解析段 | 关键字段提取 |
|---|---|---|
| `ProcessMessageHeaderSegment` | MSH | SendingApp、SendingFacility、MessageType、MessageSubType、MessageControlID、ProcessingID、MSHTimeStamp |
| `ProcessEventSegment` | EVN | 事件类型与时间 |
| `ProcessPatientIdentificationSegment` | PID | PatientID、MRN、姓名、性别、生日、种族、AccountNumber、Weight/Height |
| `ProcessPatientVisitSegment` | PV1 | PatientClass、Location、Room、Bed、AttendingPhysician、Admit/Discharge 时间、VisitNum |
| `ProcessResultSegment` | OBX | 体重（Weight）、身高（Height）的值与单位 |
| `ProcessDiagnosisSegment` | DG1 | 诊断代码与描述 |
| `ProcessMergeSegment` | MRG | PreviousPatientID、PreviousMRN、PreviousAccount、PreviousVisitNum |

**种族转换** `RaceHL7toDML()` (第 6299 行)：将 HL7 种族代码映射为 NovaNet DML 内部种族码。
**体重/身高转换** `WeightHL7toDML()` / `HeightHL7toDML()`：单位归一化（如 lb→kg、in→cm）。

---

## 七、ADT 事件类型支持

### `MessageSubTypeDescription()` (第 2374 行)
定义了所有支持的事务类型及其描述，覆盖 HL7 v2.3/2.5 规范：

| 代码 | 含义 | 代码 | 含义 |
|---|---|---|---|
| A01 | 入院 admit | A28 | 添加患者信息 |
| A02 | 转科 transfer | A31 | 更新人信息 |
| A03 | 出院/结束就诊 | A33 | 取消添加患者 |
| A04 | 登记 register | A34 | 取消合并患者 |
| A05 | 预入院 pre-admit | A35 | 取消合并账号 |
| A06 | 门诊转住院 | A36 | 取消合并就诊 |
| A07 | 住院转门诊 | A40 | 合并患者 |
| A08 | 更新患者信息 | A44 | 合并账户 |
| A09 | 患者离开（跟踪）| A45 | 合并就诊 |
| A10 | 患者到达（跟踪）| A47 | 移动账户 |
| A11 | 取消入院 | A49 | 移动患者 |
| A12 | 取消转科 | A50 | 移动就诊 |
| A13 | 取消出院 | A52 | 取消离开 |
| A17 | 患者换床（双 PID）| A53 | 取消返回 |
| A18 | 合并患者信息 | A54 | 撤销换床 |
| A21 | 请假离开 | A55 | 撤销取消入院 |
| A22 | 请假返回 | A61 | 更改身份证 |
| A23 | 删除就诊 | A62 | 撤销更改身份证 |

默认支持列表 `sDefaultSupportedTransactions` 在配置加载时确定，由 `MessageSubTypeSupported()` 校验。

---

## 八、核心业务逻辑

### 1. `ADTMessageOK()` (第 2813 行) — 消息合法性校验
- 校验必填字段（PID、PV1 等）
- 根据消息类型判断是否需要 Account、Visit、Location
- 处理 PV1-3（点位置）解析：`GetLocationAndBuildVisitNumsWithPV1`
- 无 PV1 场景下：`GetFacilityAndLocationAndBuildVisitNumsWithoutPV1`

### 2. `DetermineCrossFacilityActions()` (第 1956 行) — 跨设施场景识别
判断 4 类跨设施操作：
- **Transfer（转院）**：bCrossFacilityTransfer、bCrossFacilityTransferSameAccount、bCrossFacilityTransferSameVisit
- **Merge（合并）**：bCrossFacilityMergePatient/Account/Visit
- **Move（移动）**：bCrossFacilityMoveAccount/Visit
- 失败时调用 `CrossFacilityTransferFailure/MergeFailure/MoveFailure` 上报

### 3. `OKToAddOrUpdatePatient()` (第 3479 行) — 添加/更新前置检查
- 调用 `DetermineMergesAndMoves` 判断合并/移动意图
- 校验 MRN、PatientID、AccountNum、VisitNum 的存在性与匹配
- 设置 `bAddPatient`/`bUpdatePatient`/`bAddAccount`/`bUpdateAccount`/`bAddVisit`/`bUpdateVisit` 标志

### 4. `GetPatientVisitInfo()` (第 4773 行) — 数据库患者查询
基于 MRN/PatientID/Account/VisitNum 查询本地数据库，返回 iStatPatient/iStatAccount/iStatVisit 状态码，用于判断是新增还是更新。

### 5. `ProcessADTMessage()` (第 5263 行) — ADT 事件分发
- 通过 `m_LocationList.LookupLocNum` 解析 loc_num
- **自动出院规则匹配**：依次按 `sDischargeOPClassOrTypeByFacil` → `sDischargeOPClassOrTypeByLoc` → `sDischargeOutPatientClasses` → `sDischargeOutPatientTypes` 匹配患者类别/类型，决定 `bDischPat` 与 `iActiveHours`
- A13/A22/A52 强制清空出院时间
- 最终委托给 `AddOrUpdateAndMergePatient`

### 6. `AddOrUpdateAndMergePatient()` (第 5559 行) — 患者写库
按 `bAddPatient/bUpdatePatient/bMergePatient/bMoveVisit` 等标志执行实际的 INSERT/UPDATE，调用 `m_DBPatient` 对象操作 PATIENTS 表。

### 7. `AddOrUpdateAndMergePatientAccount()` (第 5810 行) / `AddOrUpdateAndMergePatientVisit()` (第 5974 行)
类似地处理账户表与就诊表，包含：
- `CreatePatientVisit` / `DeletePreviousPatientVisit`
- `UpdatePatientVisitIfChanged`：变更检测后增量更新
- `UpdateDischargeTime`：根据 `iActiveHours` 计算自动出院时间

### 8. `FacilityTimeToSystemTime()` (第 5789 行)
将设施本地时间转换为系统时间，依赖 `m_TimeZoneInfo`（由 `LookupTimeZone` 加载）。

---

## 九、配置管理（第 833–1870 行）

### 1. `LoadConfigFile()` (第 833 行)
- 从注册表 `HKLM\...\BinDir` 读取二进制目录
- 从数据库端口表读取端口信息 `GetPortInfo`
- 序列化到 `configdoc` (XmlDocument)，包含 Port.* 设置
- 调用 `GetOverAllConfig` / `GetFacilityConfig` / `GetLocationConfig` 分层加载

### 2. 三级配置
- **Overall**（全局）：`myOverAllConfig`
- **Facility**（机构级）：`myFacilityConfig`，通过 `GetFacilityConfig()` 加载
- **Location**（位置级）：通过 `GetLocationConfig()` 加载

关键配置参数：
- `bMRNsCrossFacilities`：MRN 是否跨设施共享
- `bAccountNumsCrossFacilities` / `bVisitNumsCrossFacilities`：账户/就诊号是否跨设施
- `sDischargeOutPatientClasses` / `sDischargeOutPatientTypes`：自动出院规则（格式 `O^24,E^24,R^24`，表示某类别 24 小时后自动出院）
- `sAdmitOnUpdateTypes`：A08 时是否触发入院
- `m_ActiveHours`：默认活跃小时数

### 3. 配置备份/恢复
`SaveOverallConfig` / `RestoreOverallConfig` / `SaveFacilityConfig` / `RestoreFacilityConfig` 用于在加载某级配置前备份、处理完恢复，避免配置污染。

---

## 十、错误处理与生命周期

### 1. `ShutDown()` (第 6713 行)
关闭流程：停止线程、关闭 DB 连接、关闭 MSMQ 句柄、上报 `m_NNBase.CommAudit(10, "Disconnect", "")`。

### 2. 异常分层处理
- `handleThreadAbortException`：线程中止（重启场景）
- `handleException`：通用异常（记录 + 可选 `bMoveMessage` 移到 reject_queue）
- `handleDBException`：OdbcException 专项处理（含事务回滚）

### 3. 消息生命周期
- `RemoveMessageFromQueue()` (第 6663 行)：成功处理后从主队列删除
- `MoveMessageToSubQueue()` (第 6685 行)：处理失败时通过 `MQMoveMessage` 移到 reject_queue

### 4. `ProcessNotify(int cd)` (第 636 行) — 外部通知
- `cd=1`：开启日志
- `cd=2`：停止日志
- `cd=-1`：触发 `ShutDown`（不退出进程）

---

## 十一、辅助工具方法

| 方法 | 作用 |
|---|---|
| `YMDhms_To_DateTime` (6418) | HL7 时间字符串 `yyyyMMddHHmmss` 转 DateTime，校验年份 1800–2037 |
| `DateTime2HL7` (6651) | 反向转换 |
| `GetHL7Field` / `GetHL7Component` (6615/6626) | 按字段号/组件号提取 HL7 值 |
| `FindHL7Field` / `FindHL7Component` | 定位字段位置 |
| `GetHL7Length` | 计算字段长度（用于截断） |
| `isNumeric` | 数值校验 |
| `RemoveAsciiControlChar` (6985) | 日志输出前移除 VT/FS/STX/ETX 控制字符 |
| `LookupFacilName` / `LookupPrevFacilName` | 按 facil_num 反查设施名 |
| `LookupTimeZone` (3377) | 加载时区信息 |

---

## 十二、整体架构总结

```
┌─────────────────────────────────────────────────────────────┐
│                     HIS (医院信息系统)                       │
└──────────────────────────┬──────────────────────────────────┘
                           │ HL7 ADT 消息 (MSH|EVN|PID|PV1|OBX|DG1|MRG)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              MSMQ 事务队列 (RTMADTQ)                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ PeekCompleted 异步回调
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  HL7Protocol.GetMSMQMessage → ProcessMessage                │
│    ├── 两遍扫描段解析                                        │
│    ├── 多 PID/MRG 拆分（A17/A18/A40/A44/A45/A47）           │
│    └── ProcessParsedMessage / ProcessMessageForFacility     │
│           ├── ADTMessageOK (校验)                           │
│           ├── DetermineCrossFacilityActions (跨设施判定)    │
│           ├── OKToAddOrUpdatePatient (CRUD 标志)            │
│           ├── ProcessADTMessage (自动出院规则匹配)          │
│           └── AddOrUpdateAndMerge{Patient/Account/Visit}    │
└──────────┬───────────────────────┬──────────────────────────┘
           │                       │
           ▼                       ▼
┌──────────────────┐    ┌──────────────────────────────────┐
│ NovaNet 主库     │    │ Profile_Track 库                  │
│ (Sybase ODBC)    │    │ (PatientTrackingRec 跟踪审计)     │
│ PATIENTS         │    └──────────────────────────────────┘
│ PATIENT_ACCT     │
│ PATIENT_VISIT    │
│ FACILITY/LOCATION│
└──────────────────┘
                           │
                           ▼
                  后续 DML 设备查询此数据
                  用于患者匹配与样本采集
```

---

## 十三、与 jnet-nova 项目的关联

结合项目记忆中 DML 协议状态机的实现，可以看出 `HL7Protocol` 是 NovaNet 的**上游数据源**：

1. **数据流向**：HIS → MSMQ → HL7Protocol → NovaNet DB → DML Server → 生化分析仪
2. **表对应关系**：`HL7Protocol` 写入的 PATIENTS/PATIENT_VISIT 等表，正是 [novanet.sql](file:///e:/doc/jnet-nova/sql/novanet.sql) 中定义的表结构，也是您之前创建 [novanet_init_data.sql](file:///e:/doc/jnet-nova/sql/novanet_init_data.sql) 初始化数据的目标表
3. **业务字段映射**：HL7 字段（PID-3、PV1-3、PV1-19 等）→ NovaNet 表字段（patient_uuid、loc_num、visit_uuid 等）→ DML 消息字段（DML Protocol 中的 DST/ROBS/RDEV 主题）
4. **配置一致性**：HL7Protocol 的 `bMRNsCrossFacilities`、`sDischargeOutPatientClasses` 等规则，与 Java 端 `PatientSyncService` 的同步策略需要在业务上保持一致

理解 `HL7Protocol` 有助于在 Java 端重建患者同步逻辑时，对齐 HIS 数据语义（特别是 A08 更新、A06/A07 门诊住院转换、A40/A47 合并移动等复杂场景的字段变更规则）。