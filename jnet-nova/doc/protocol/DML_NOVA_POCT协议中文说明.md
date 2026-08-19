# Nova NovaNet DML 接口协议规范 — 中文详细文档

> **文档来源**：DML_Novanet Interface Specs v2.59 (Sept 12, 2012)
> **协议基础**：NCCLS POCT1-A (Point-of-Care Connectivity) 标准
> **适用设备**：StatStrip / StatStrip79 / StatStrip32 / StatSensor39 / StatSensor39i

---

## 一、文档概述

### 1.1 目的

本文档是 Nova Biomedical 公司 **NovaNet** 系统对 **POCT1-A DML（Device Messaging Layer，设备消息层）** 协议的实现规范。它描述了 **仪器控制器/观测集中器（NovaNet）** 与 **DML 设备**（实验室计算机、分析仪、血糖仪等）之间传输消息的详细信息。

### 1.2 核心术语

| 术语 | 全称 | 中文含义 |
|------|------|----------|
| **NovaNet** | Nova Biomedical's Instrument Controller | Nova 仪器控制器/观测集中器 |
| **DML** | Device Messaging Layer | 设备消息层 |
| **POCT1-A** | Point-of-Care Connectivity | 即时检验连接标准 |
| **HIS** | Hospital Information System | 医院信息系统 |
| **LIS** | Laboratory Information System | 实验室信息系统 |
| **Result Server** | — | 接收 NovaNet 观测结果并转发至 LIS/HIS 的第三方系统 |
| **Operator List Server** | — | 维护操作员列表并下发至 NovaNet 的第三方系统 |
| **POC** | Point-of-Care | 即时检验/床旁检测 |
| **MRN** | Medical Record Number | 病历号 |

### 1.3 支持的设备类型

| 设备 | 测量项目 | 测试代码 | 单位 |
|------|----------|----------|------|
| **StatStrip** | Glu（葡萄糖） | 2341-6 (LN) | mg/dL (mmol/L) |
| **StatSensor39/39i** | Creat（肌酐） | CRESS (NOVABIO) | mg/dL (μmol/L) |
| **StatStrip79** | Glu + Ket（葡萄糖+酮体） | 2341-6 / 53061-8 | mg/dL / mmol/L |
| **StatStrip32** | Lac（乳酸） | SSLAC (NOVABIO) | mmol/L |
| 所有设备 | eGFR（计算值） | CREGFR (NOVABIO) | mL/min/1.73m² |

### 1.4 编码与格式约定

- **消息编码**：UTF-8（支持 ASCII 之外的字符）
- **小数分隔符**：统一使用 **小数点**（非逗号）
- **XML 格式**：所有消息均基于 XML 结构
- **属性标记约定**：`+` 必填、`-` 可选、`#` NovaNet 要求必填

---

## 二、通信流程图

### 2.1 基本消息配置文件（Basic Messaging Profile）

```
┌──────────────┐                              ┌──────────────┐
│   DML 设备    │                              │   NovaNet    │
│ (血糖仪等)    │                              │ (控制器/集中器)│
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │  ① HEL.R01 (Hello 消息)                     │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ② ACK (确认)                               │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ③ DST.R01 (设备状态消息)                    │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ④ ACK                                      │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑤ Request Observations (请求观测数据)       │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑥ OBS.R01/R02 (观测消息) 或 EOT.R01       │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ⑦ ACK                                      │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑧ 重复 ⑥⑦ 直到所有观测数据发送完毕         │
       │     最后发送 EOT.R01 (主题结束)              │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ⑨ Request Device Events (请求设备事件)      │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑩ EVS.R01 (设备事件消息) 或 EOT.R01        │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ⑪ ACK                                      │
       │<─────────────────────────────────────────────│
```

### 2.2 列表下发阶段

```
       │                                             │
       │  ⑫ 下发列表消息 (操作员/患者/试剂/设置等)     │
       │     OPL.R01/R02, PTL.R01/R02,               │
       │     NOVA.REAG, NOVA.LOC, NOVA.PHYS,         │
       │     NOVA.STATSTRIP.SETUP, NOVA.FRM          │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑬ ACK (确认每条列表消息)                    │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ⑭ EOT.R01 (列表主题结束)                   │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑮ ACK                                      │
       │─────────────────────────────────────────────>│
```

### 2.3 连续模式（Continuous Mode）

```
       │                                             │
       │  ⑯ DTV.R01 (START_CONTINUOUS 指令)          │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑰ ACK (进入连续模式)                        │
       │─────────────────────────────────────────────>│
       │                                             │
       │  ══════════ 连续模式运行中 ══════════════    │
       │                                             │
       │  设备可随时发送:                              │
       │  • OBS.R01/R02 (观测结果)                   │
       │  • EVS.R01 (设备事件)                       │
       │  • Keep Alive (心跳)                        │
       │─────────────────────────────────────────────>│
       │                                             │
       │  NovaNet 可随时下发:                         │
       │  • 指令 (Directive)                         │
       │  • 各类列表更新                              │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑱ END.R01 (终止消息)                        │
       │<─────────────────────────────────────────────│
       │                                             │
       │  ⑲ ACK + 断开连接                           │
       │─────────────────────────────────────────────>│
```

### 2.4 第三方接口流程

```
┌────────────┐         ┌──────────────┐         ┌──────────────┐
│ 第三方系统  │         │   NovaNet    │         │   DML 设备    │
│ (LIS/HIS)  │         │   (中间件)    │         │  (血糖仪)     │
└─────┬──────┘         └──────┬───────┘         └──────┬───────┘
      │                       │                        │
      │  结果接口 (Result Interface)                    │
      │  NovaNet 模拟设备向第三方推送结果               │
      │                       │                        │
      │  HEL.R01 (含设备标识)  │  设备上传观测结果       │
      │<──────────────────────│<───────────────────────│
      │  DST.R01              │                        │
      │<──────────────────────│                        │
      │  OBS.R01 (患者结果)   │                        │
      │<──────────────────────│                        │
      │  ACK                  │                        │
      │──────────────────────>│                        │
      │  END.R01 (会话结束)   │                        │
      │<──────────────────────│                        │
      │                       │                        │
      │  操作员列表接口 (Operator List Interface)       │
      │  第三方向 NovaNet 推送操作员列表                │
      │                       │                        │
      │  HEL.R01              │                        │
      │──────────────────────>│                        │
      │  DST.R01              │                        │
      │<──────────────────────│                        │
      │  OPL.R01/R02          │                        │
      │──────────────────────>│  下发操作员列表到设备   │
      │  EOT.R01              │───────────────────────>│
      │──────────────────────>│                        │
```

---

## 三、消息类型总览

| 消息类型 | 消息代码 | 方向 | 说明 |
|----------|----------|------|------|
| **Hello** | `HEL.R01` | 设备 → NovaNet | 连接建立时发送，标识设备及其能力 |
| **Device Status** | `DST.R01` | 设备 → NovaNet | 报告设备当前状态和各类数据更新时间 |
| **Patient Observation** | `OBS.R01` | 设备 → NovaNet | 上传患者检测结果 |
| **Non-Patient Observation** | `OBS.R02` | 设备 → NovaNet | 上传 QC/校准/线性等非患者结果 |
| **Operator List (完整)** | `OPL.R01` | NovaNet → 设备 | 完整操作员列表下发 |
| **Operator List (增量)** | `OPL.R02` | NovaNet → 设备 | 增量操作员列表更新 |
| **Patient List (完整)** | `PTL.R01` | NovaNet → 设备 | 完整患者列表下发 |
| **Patient List (增量)** | `PTL.R02` | NovaNet → 设备 | 增量患者列表更新 |
| **Device Events** | `EVS.R01` | 设备 → NovaNet | 上报设备事件（维护/错误/消息已读） |
| **Basic Directive** | `DTV.R01` | NovaNet → 设备 | 基本指令（如启动连续模式） |
| **Complex Directive** | `DTV.R02` | NovaNet → 设备 | 复杂指令（如设置时间） |
| **End of Topic** | `EOT.R01` | 双向 | 标记某一主题所有消息已发送完毕 |
| **Terminate** | `END.R01` | NovaNet → 设备 | 终止当前会话 |
| **Reagent List** | `NOVA.REAG` | NovaNet → 设备 | 试剂/质控品批次列表 |
| **Location List** | `NOVA.LOC` | NovaNet → 设备 | 设施和位置列表 |
| **Physician List** | `NOVA.PHYS.R01/R02` | NovaNet → 设备 | 医师列表 |
| **Firmware Update** | `NOVA.FRM` | NovaNet → 设备 | 固件升级数据 |
| **Setup** | `NOVA.STATSTRIP.SETUP` | NovaNet → 设备 | 设备完整配置（键值对+测试配置+注释+诊断码） |

---

## 四、核心消息格式与字段详解

### 4.1 Header 对象 (HDR) — 所有消息的必备头

每条消息必须包含 HDR 对象：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message_type` | CV | - | 消息类型，如 `HEL.R01`、`OBS.R01` |
| `control_id` | ST | + | 消息控制 ID（用于 ACK 关联） |
| `version_id` | ST | + | 协议版本标识，固定为 `POCT1` |
| `creation_dttm` | TS | + | 消息创建时间（ISO 8601 格式） |

**示例**：
```xml
<HDR>
  <HDR.message_type V="HEL.R01" SN="POCT1" SV="1" />
  <HDR.control_id V="7135" />
  <HDR.version_id V="POCT1" />
  <HDR.creation_dttm V="2010-02-22T14:06:34.00-05:00" />
</HDR>
```

---

### 4.2 Hello 消息 (HEL.R01)

**用途**：设备连接后发送的第一条消息，用于标识设备身份和通信能力。

#### 4.2.1 Device 对象 (DEV)

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `device_id` | ST | + | 设备 ID。文本节点包含 `facility^location`（如 `MGH^ICU`） |
| `vendor_id` | ST | - | 厂商 ID，如 `NOVABIO` |
| `model_id` | ST | # | 设备型号：`StatStrip` / `StatSensor39` / `StatSensor39i` / `StatStrip79` / `StatStrip32` |
| `serial_id` | ST | # | 设备序列号 |
| `manufacturer_name` | ON | - | 制造商名称 |
| `hw_version` | ST | - | 硬件版本。一代设备为空，二代格式为 `Kxxxxx` |
| `sw_version` | ST | # | 软件版本（四段式如 `2.0.5.10`）。文本节点含语言代码（如 `3.10.8.0_it-IT`） |
| `device_name` | ST | # | 设备名称 |

#### 4.2.2 Device Capabilities 对象 (DCP)

| 属性 | 类型 | 说明 |
|------|------|------|
| `application_timeout` | REAL | 应用层超时时间（秒） |

#### 4.2.3 Device Static Capabilities 对象 (DSCP)

| 属性 | 说明 |
|------|------|
| `connection_profile_cd` | 连接配置文件，固定为 `SA`（同步确认） |
| `topics_supported_cd` | 支持的主题列表（见下表） |
| `directives_supported_cd` | 支持的指令：`START_CONTINUOUS`、`SET_TIME` |
| `max_message_sz` | 最大消息大小（字节） |

**Nova 自定义主题代码**：

| 代码 | 说明 |
|------|------|
| `NOVA.STATSTRIP.SETUP` | 设备配置主题 |
| `NOVA.LOC` | 位置列表主题 |
| `NOVA.PHYS` / `NOVA.PHYS_I` | 医师列表（完整/增量） |
| `NOVA.REAG` | 试剂列表主题 |
| `NOVA.FRM` | 固件升级主题 |
| `NOVA.MANUAL_TEST` | 手动测试 (MTE) 主题 |

**完整示例**：
```xml
<HEL.R01>
  <HDR>
    <HDR.message_type V="HEL.R01" SN="POCT1" SV="1" />
    <HDR.control_id V="7135" />
    <HDR.version_id V="POCT1" />
    <HDR.creation_dttm V="2010-02-22T14:06:34.00-05:00" />
  </HDR>
  <DEV>
    <DEV.device_id V="">MGH^ICU</DEV.device_id>
    <DEV.vendor_id V="NOVABIO" />
    <DEV.model_id V="StatStrip" />
    <DEV.serial_id V="0600100736" />
    <DEV.hw_version V="K1" />
    <DEV.sw_version V="3.10.8.0">3.10.8.0_it-IT</DEV.sw_version>
    <DEV.device_name V="DLO-OV" />
    <DCP>
      <DCP.application_timeout V="120" />
    </DCP>
    <DSC>
      <DSC.connection_profile_cd V="SA" />
      <DSC.topics_supported_cd V="D_EV" SN="POCT1" SV="1" />
      <DSC.topics_supported_cd V="NOVA.STATSTRIP.SETUP" SN="POCT1" SV="1" />
      <DSC.topics_supported_cd V="NOVA.REAG" SN="POCT1" SV="1" />
      <DSC.directives_supported_cd V="START_CONTINUOUS" SN="POCT1" SV="1" />
      <DSC.max_message_sz V="32768" />
    </DSC>
  </DEV>
</HEL.R01>
```

---

### 4.3 Device Status 消息 (DST.R01)

**用途**：Hello 确认后，设备报告当前状态及各类数据的最后更新时间。

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status_dttm` | TS | + | 当前状态时间戳 |
| `new_observations_qty` | INT | + | 待上传的新观测结果数量 |
| `new_events_qty` | INT | - | 待上报的新事件数量 |
| `condition_cd` | CV | + | 设备条件代码（如 `R` = Ready） |
| `observations_update_dttm` | TS | # | 观测数据最后更新时间 |
| `events_update_dttm` | TS | # | 事件最后更新时间 |
| `operators_update_dttm` | TS | # | 操作员列表最后更新时间 |
| `patients_update_dttm` | TS | # | 患者列表最后更新时间 |
| `loc_list_update_dttm` | TS | # | 位置列表最后更新时间（Nova 扩展） |
| `phys_update_dttm` | TS | # | 医师列表最后更新时间（Nova 扩展） |
| `reag_update_dttm` | TS | # | 试剂列表最后更新时间（Nova 扩展） |
| `setup_update_dttm` | TS | # | 配置最后更新时间（Nova 扩展） |

**示例**：
```xml
<DST.R01>
  <HDR>
    <HDR.message_type V="DST.R01" SN="POCT1" />
    <HDR.control_id V="1019" />
    <HDR.version_id V="POCT1" />
    <HDR.creation_dttm V="2007-04-12T18:53:04.00-05:00" />
  </HDR>
  <DST>
    <DST.status_dttm V="2007-04-12T18:53:04.00-05:00" />
    <DST.new_observations_qty V="0" />
    <DST.new_events_qty V="0" />
    <DST.condition_cd V="R" SN="POCT1" />
    <DST.observations_update_dttm V="2000-01-01T00:00:00.00-05:00" />
    <DST.operators_update_dttm V="2007-04-12T18:49:09-05:00" />
    <DST.setup_update_dttm V="2007-04-12T18:49:07-05:00" />
  </DST>
</DST.R01>
```

---

### 4.4 患者观测消息 (OBS.R01)

**用途**：上传患者检测结果。这是最核心的业务消息。

#### 消息结构层次

```
OBS.R01
├── HDR (消息头)
└── SVC (Service 对象, 1…*)
    ├── PT (Patient 患者对象)
    │   ├── OBS (Observation 观测对象, 1…*)
    │   │   └── NTE (Note 备注, 0…*, 如 GFR 数据)
    │   └── (患者信息)
    ├── OPR (Operator 操作员对象, 0…1)
    ├── ORD (Order 医嘱对象, 0…1)
    ├── SPC (Specimen 标本对象, 0…1)
    ├── RGT (Reagent 试剂对象, 0…*)
    └── NTE (Note 备注对象, 0…*, 样本级注释)
```

#### 4.4.1 Service 对象 (SVC)

| 属性 | 类型 | 说明 |
|------|------|------|
| `role_cd` | CS | 角色代码（`OBS` = 观测） |
| `observation_dttm` | TS | 观测时间 |
| `status_cd` | CS | `NRM` = 正常条件；`OVR` = 有主管覆盖或 QC 过期时测试 |

#### 4.4.2 Patient 对象 (PT)

| 属性 | 类型 | 说明 |
|------|------|------|
| `patient_id` | ST | 患者 ID / MRN / 账号。若为 accession number 则填 `UNKNOWN` |
| `location` | ST | 采样时患者位置，格式：`facility^location` |

#### 4.4.3 Observation 对象 (OBS)

| 属性 | 类型 | 说明 |
|------|------|------|
| `observation_id` | CE | 测试名称（如 `Glu`、`Creat`）或 LOINC 代码（如 `2341-6`） |
| `value` | PQ | 定量结果值，如 `V="145" U="mg/dL"`。用户取消时 `NULL="NI"` |
| `qualitative_value` | CV | 定性结果 |
| `method_cd` | CS | 方法代码（`M` = 测量、`C` = 计算、`I` = 手动输入） |
| `status_cd` | CS | 状态代码（`A` = 已确认） |
| `interpretation_cd` | CS | 解释代码：`N`=正常、`L`=偏低、`H`=偏高、`LL`=危急低、`HH`=危急高 |
| `normal_lo-hi_limit` | IVL\ | 参考范围，如 `[80;120]` |
| `critical_lo-hi_limit` | IVL\ | 危急值范围，如 `[60;140]` |

#### 4.4.4 Operator 对象 (OPR)

| 属性 | 类型 | 说明 |
|------|------|------|
| `operator_id` | ST | 操作员 ID。主管覆盖时格式为 `operator_id^supervisor_id` |

#### 4.4.5 Order 对象 (ORD)

| 属性 | 类型 | 说明 |
|------|------|------|
| `universal_service_id` | CE | 手动测试时为测试名称，否则为 `CHEM` |
| `ordering_provider_id` | ST | 开医嘱医师 ID |
| `order_id` | CV | Accession number |

#### 4.4.6 Specimen 对象 (SPC)

| 属性 | 类型 | 说明 |
|------|------|------|
| `specimen_dttm` | TS | 采样时间 |
| `type_cd` | CE | 标本类型（如 `BLD` = 全血） |

#### 4.4.7 Reagent 对象 (RGT)

| 属性 | 说明 |
|------|------|
| `name` | 批次名称（试纸条为空字符串）。文本节点含试剂类型：`TY=TS/QC/LN/PRO/RG/MT_TS/MT_QC/MT_DE` |
| `lot_number` | 批号 |
| `expiration_date` | 有效期 |

**试剂类型代码**：

| 代码 | 含义 |
|------|------|
| `TS` | 测试试纸 |
| `QC` | 质控液 |
| `LN` | 线性验证品 |
| `PRO` | 能力验证品 |
| `RG` | 试剂 |
| `MT_TS` | 手动测试卡/试纸 |
| `MT_QC` | 手动测试质控品 |
| `MT_DE` | 手动测试显色液 |

#### 4.4.8 Note 对象 (NTE) — 样本级注释

Note 对象用于传递多种附加信息，通过 `V` 属性值区分类型：

| V 属性值 | 用途 | 文本节点内容 |
|----------|------|-------------|
| 注释文本 | 样本注释 | `TY=C^CH={0,1}^FL={0,1}`（类型^可记录^可标记） |
| `ID FLAGS` | 各 ID 的状态标记 | `OPR_ID=flag^SPVR_ID=flag^SMPL_ID=flag^PHYS_ID=flag` |
| `SAMPLE ID TYPE` | 样本 ID 类型 | `PATID` / `MRN` / `ACCT` |
| `DIAGCODE` | 诊断码 | 诊断码值（如 `250.01`） |
| `TGC FLAG` | 强化血糖控制标志 | `1` = 是，`0` = 否 |

**ID Flags 有效值**：`DONT_CARE`、`ENTERED_TEXT`、`SCANNED_TEXT`、`IN_LIST`、`NOT_IN_LIST`、`NEW_ID`、`DOWN_TIME`

#### 完整 OBS.R01 示例

```xml
<OBS.R01>
  <HDR>
    <HDR.message_type V="OBS.R01"/>
    <HDR.control_id V="10003"/>
    <HDR.version_id V="POCT1"/>
    <HDR.creation_dttm V="2001-11-01T16:30:06-05:00"/>
  </HDR>
  <SVC>
    <SVC.role_cd V="OBS"/>
    <SVC.observation_dttm V="2001-11-01T16:29:54-05:00"/>
    <SVC.status_cd V="NRM" />
    <PT>
      <PT.patient_id V="PT222-55-7777"/>
      <PT.location V="Cambridge Hosp^ICU-4"/>
      <OBS>
        <OBS.observation_id V="2341-6" DN="Glu" SN="LN" />
        <OBS.value V="145" U="mg/dL"/>
        <OBS.method_cd V="M"/>
        <OBS.status_cd V="A"/>
        <OBS.interpretation_cd V="HH" />
        <OBS.normal_lo-hi_limit V="[80;120]" U="mg/dL"/>
        <OBS.critical_lo-hi_limit V="[60;140]" U="mg/dL"/>
      </OBS>
    </PT>
    <OPR>
      <OPR.operator_id V="OP777-88-9999"/>
    </OPR>
    <ORD>
      <ORD.universal_service_id V="CHEM" />
      <ORD.ordering_provider_id V="120" />
    </ORD>
    <SPC>
      <SPC.specimen_dttm V="2001-11-01T16:27:00-05:00"/>
      <SPC.type_cd V="BLD"/>
    </SPC>
    <RGT>
      <RGT.name V="">TY=TS</RGT.name>
      <RGT.lot_number V="0310010249" />
      <RGT.expiration_date V="2012-01-31T00:00:00.00-05:00" />
    </RGT>
    <NTE>
      <NTE.text V="New strip">TY=C^CH=1^FL=0</NTE.text>
    </NTE>
    <NTE>
      <NTE.text V="ID FLAGS">
        OPR_ID=IN_LIST^
        SPVR_ID=ENTERED_TEXT|NOT_IN_LIST^
        SMPL_ID=ENTERED_TEXT|NOT_IN_LIST|DOWN_TIME^
        PHYS_ID=IN_LIST
      </NTE.text>
    </NTE>
    <NTE>
      <NTE.text V="SAMPLE ID TYPE">MRN</NTE.text>
    </NTE>
    <NTE>
      <NTE.text V="TGC FLAG">1</NTE.text>
    </NTE>
    <NTE>
      <NTE.text V="DIAGCODE">250.01</NTE.text>
    </NTE>
  </SVC>
</OBS.R01>
```

---

### 4.5 非患者观测消息 (OBS.R02)

**用途**：上传 QC、校准、能力验证、线性测试等非患者结果。

结构与 OBS.R01 类似，但用 **CTC（Control/Calibration）对象** 代替 PT 对象：

| CTC 属性 | 说明 |
|----------|------|
| `name` | 批号（QC/线性/能力验证品无批次名称） |
| `lot_number` | 批号 |
| `expiration_date` | 有效期 |
| `level_cd` | QC 水平级别（1-4） |

`SVC.role_cd` 取值：`LQC`（液体质控）、`PRF`（能力验证）等。

---

### 4.6 操作员列表消息 (OPL.R01/R02)

| 消息类型 | 说明 |
|----------|------|
| `OPL.R01` | 完整更新（全量下发） |
| `OPL.R02` | 增量更新（仅变更部分，变更数 < 200 时使用） |

#### 核心对象

**Update Action (UPD)**：`action_cd` = `I`（插入/更新）或 `D`（删除）

**Operator (OPR)**：

| 属性 | 说明 |
|------|------|
| `operator_id` | 操作员唯一标识。第三方接口时文本节点含 `facility^location^inst_type` |
| `name` | 操作员姓名（First/Last 限 16 字符） |

**Access Control (ACC)**：

| 属性 | 说明 |
|------|------|
| `method_cd` | 认证测试项目：`Glu`、`Creat`、`Ket`、`Lac`、`MTE`（全部手动测试） |
| `password` | 密码 |
| `active_date` | 生效日期 |
| `expiration_date` | 过期日期 |
| `permission_level_cd` | 权限级别 |

**权限级别对照表**：

| 代码 | 含义 |
|------|------|
| 1 | Supervisor（主管） |
| 2 | Admin（管理员） |
| 4 | User（普通操作员） |
| 7 | Supervisor + GFR 权限 |
| 8 | Operator + GFR 权限 |
| 16 | Admin 用户 |

---

### 4.7 患者列表消息 (PTL.R01/R02)

| 消息类型 | 说明 |
|----------|------|
| `PTL.R01` | 完整更新 |
| `PTL.R02` | 增量更新（变更数 < 200） |

**Patient (PT) 对象**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `patient_id` | ST | 患者标识。文本节点含 ID 类型：`PATID` / `MRN` / `ACCTNO` |
| `location` | ST | 位置：`facility^location^room^bed` |
| `name` | PN | 姓名（First/Last/Mid ≤ 16 字符，Prefix/Suffix ≤ 4 字符） |
| `birth_date` | TS | 出生日期 |
| `gender_cd` | CS | 性别代码 |

**Note (NTE)**：可附带医师备注（Physician's Note），显示在患者确认界面。

---

### 4.8 设备事件消息 (EVS.R01)

**用途**：上报操作员消息已读、维护动作、系统错误。

| EVT 属性 | 说明 |
|----------|------|
| `description` | 事件描述。`V` 属性含事件文本，文本节点含 `TY=MT`（维护）或 `TY=SE`（系统错误）；消息已读时 `V="OP MSG READ"` |
| `event_dttm` | 事件发生时间 |
| `severity_cd` | 严重级别：`N`=正常、`W`=警告、`C`=严重 |

---

### 4.9 指令消息

#### Basic Directive (DTV.R01)
```xml
<DTV.R01>
  <HDR>...</HDR>
  <DTV>
    <DTV.command_cd V="START_CONTINUOUS" />
  </DTV>
</DTV.R01>
```

#### Complex Directive (DTV.R02) — 设置时间
```xml
<DTV.R02>
  <HDR>...</HDR>
  <DTV>
    <DTV.command_cd V="SET_TIME" />
  </DTV>
  <TM>
    <TM.dttm V="2010-03-05T11:06:00-05:00" />
  </TM>
</DTV.R02>
```

---

### 4.10 End of Topic (EOT.R01) 与 Terminate (END.R01)

**EOT.R01**：标记某一主题所有消息已发送完毕。

| 属性 | 说明 |
|------|------|
| `topic_cd` | 已完成的主题代码 |
| `update_dttm` | 最后更新时间（接收方记录） |

**END.R01**：终止当前会话，可指定自动重连时间。

| 属性 | 说明 |
|------|------|
| `reason_cd` | 终止原因（`NRM` = 正常） |
| `note_txt` | `RECONNECT:XX`（XX = 05-99 分钟后重连） |

---

### 4.11 试剂列表消息 (NOVA.REAG)

```
NOVA.REAG
├── HDR
└── LOT (1…*)
    ├── Level (0…*, QC/线性品需要)
    │   └── TST (1…*, 参考范围)
    └── (无 Level: 试纸条/手动测试品)
```

**LOT 对象属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `lot_number` | INT | 批号（最长 10 字符） |
| `type` | CS | 批次类型：`TS`/`QC`/`LN`/`PRO`/`RG`/`MT_TS`/`MT_QC`/`MT_DE` |
| `expiration_dttm` | TS | 有效期 |
| `name` | ST | 批次名称（MTE 相关品必填，最长 16 字符） |

---

### 4.12 位置列表 (NOVA.LOC)、医师列表 (NOVA.PHYS)、固件升级 (NOVA.FRM)

**位置列表**：
```xml
<NOVA.LOC>
  <HDR>...</HDR>
  <LOC>
    <LOC.facility V="Boston">
      <unit V="ICU" DF="F"/>   <!-- DF=F 非默认, DF=T 默认 -->
      <unit V="ER" DF="T"/>
    </LOC.facility>
  </LOC>
</NOVA.LOC>
```

**固件升级**：数据为 Base64 编码的二进制块，文件名格式为 `Meter_ID_MM_mm_bb_rr_ll-RR.b64`。

---

### 4.13 设备配置消息 (NOVA.STATSTRIP.SETUP)

**这是最复杂的消息**，包含四大模块：

```
NOVA.STATSTRIP.SETUP
├── HDR
├── KEY_VALUE (键值对配置, 有且仅有一个)
├── TEST_CONFIG (测试配置, 有且仅有一个)
├── QC_CONFIG (QC 配置, 有且仅有一个)
├── COMMENTS (预定义注释, 有且仅有一个)
└── DIAGCODES (诊断码, 有且仅有一个)
```

> ⚠️ **完整性规则**：KeyValues 和 Test Config 中任何一项无效，将导致两个模块的所有设置全部被丢弃。所有键值对和测试配置必须在 Setup 主题中完整下发。

#### 4.13.1 KeyValues（键值对）

格式：
```xml
<KEY_VALUE>
  <KeyName1 V="KeyValue1" />
  <KeyName2 V="KeyValue2" />
  ...
</KEY_VALUE>
```

**键名和键值各最长 32 字符**。主要键值分类：

| 类别 | 典型键名 | 说明 |
|------|----------|------|
| **操作员登录** | `OpLoginMaxLength`, `OpLoginScanEnableCd`, `OpLogoffModeCd` | 控制操作员 ID 输入方式、自动注销 |
| **患者 ID** | `PatIdMaxLength`, `PatIdTypeCd`, `PatIdAutoEnabled` | 患者 ID 格式、自动输入 |
| **Accession ID** | `AccnIdMaxLength`, `AccnIdScanEnableCd` | 条码扫描配置 |
| **医师 ID** | `PhysIdPromptEnable`, `PhysIdMaxLength` | 医师 ID 输入控制 |
| **QC 锁定** | `QcLockModeCd`, `QcLockElapsedHrs`, `QcLockLevel1-4Req` | QC 锁定策略 |
| **条形码扫描** | `*ScanEnableCbar/C128/C39/C93/C2o5/2D` | 各条码类型启用/禁用 |
| **结果管理** | `ObsRejectEnable`, `ObsAbnormalRangeCommentReq` | 结果拒绝、异常注释要求 |
| **手动测试 (MTE)** | `MTETestValidation`, `MTEAllowExpTLotOverride` | 手动测试批号验证 |
| **GFR 计算** | `GFRAdultMethodCd`, `GFRAdolescentMethodCd` | GFR 计算公式选择 |
| **Dock 锁定** | `DockLockModeCd`, `DockLockElapsedHrs` | 底座锁定策略 |
| **日期时间** | `DateFormat`, `TimeFormat` | 显示格式 |
| **权限级别** | `PrivLevelSetDateTimeCd`, `PrivLevelTesttypeLinearityCd` | 各操作的权限要求 |

#### 4.13.2 测试配置 (TEST_CONFIG)

```xml
<TEST_CONFIG>
  <TEST TN="Glu" RT="M" U="mg/dL" NF="XXX.X" SL="1.00" IC="0" ED="0">
    <RANGE RF="[80;120]" CT="[70;200]" SEX="U" ABS="[10;600]" 
           CODE="2341-6" CODE_SYS="LN" />
  </TEST>
  <TEST TN="eGFR" RT="C" U="mL/min/1.73 m2">
    <RANGE RF="[15;70]" CT="[10;85]" EQ="MDRD" CODE="CREGFR" CODE_SYS="NOVABIO" />
    <RANGE AGE="[1;365]" LABEL="Pre-Term Infant" EQ="SZ" EQ_CONST="0.33" />
  </TEST>
</TEST_CONFIG>
```

**`<TEST>` 属性**：

| 属性 | 说明 |
|------|------|
| `TN` | 测试名称（如 `Glu`、`Creat`、`eGFR`） |
| `RT` | 结果类型：`M`=测量、`C`=计算、`I`=手动 |
| `U` | 单位 |
| `NF` | 数值格式（如 `XXX.X`） |
| `SL` | 斜率 |
| `IC` | 截距 |
| `ED` | 允许/禁止在设备上取消选择该测试（`1`/`0`） |

**`<RANGE>` 属性**：

| 属性 | 说明 |
|------|------|
| `RF` | 参考范围 `[low;high]` |
| `CT` | 危急值范围 |
| `ABS` | 技术范围（绝对范围） |
| `SEX` | 性别分组（`M`/`F`/`U`） |
| `AGE` | 年龄范围（单位：天） |
| `RACE` | 种族分组（`W`/`B`/`NB`/`O`/`NA`/`H`） |
| `LABEL` | 年龄组标签（如用于 Schwartz 公式） |
| `EQ` | GFR 计算公式：`CG`/`MDRD`/`MDRD-IDMS`/`SZ`/`CB` |
| `EQ_CONST` | Schwartz 公式的 K 常数 |
| `CODE` / `CODE_SYS` | 替代测试代码及编码系统 |

#### 手动测试 (MTE) 配置结构

```
TEST (TN="Panel Name", RT="I")
├── MANUAL_TEST (患者测试)
│   ├── SUB_TEST (子测试 1)
│   │   ├── TEST_ENTRY (MDT=NUMERIC/SELECT_LIST/TEXT)
│   │   │   ├── SELECT_VALUE (选择列表值)
│   │   │   └── RANGE (数值范围)
│   │   └── CONTROL_ENTRY (内部 QC)
│   └── SUB_TEST (子测试 2)
│       └── ...
└── MANUAL_QC_TEST (QC 测试, 0…3)
    └── SUB_TEST
        └── ...
```

#### 4.13.3 QC 配置 (QC_CONFIG)

> 注：文档标注此部分 **未实现 (NOT IMPLEMENTED)**。

支持三种 QC 类型配置：
- `<QC>` — 液体质控
- `<LINEARITY>` — 线性验证
- `<PROFICIENCY>` — 能力验证

每种类型支持三种时间间隔模式：
- `SHIFT`：按班次时间（如 `0100*0900*1700`）
- `ELAPSED`：按经过小时数（如 `8` 小时）
- `EXACT`：按精确日期时间

QC 模式：`NONE`（不配置）、`NOTIFY`（通知）、`LOCK`（锁定设备）

#### 4.13.4 注释与诊断码

**预定义注释**：
```xml
<COMMENTS>
  <Comment V="Notified Doctor" TN="Glu" CH="1" FL="0" CT="PT" />
  <Comment V="Wrong Control used" TN="Glu" CH="0" FL="0" CT="QC" />
</COMMENTS>
```

| 属性 | 说明 |
|------|------|
| `V` | 注释文本（最长 20 字符） |
| `TN` | 关联测试名称 |
| `CT` | 注释类型：`QC`/`PT`/`MT`/`LN`/`PF`/`CR` |
| `CH` | 是否可记录（`0`/`1`） |
| `FL` | 是否可标记（`0`/`1`） |

**诊断码**：
```xml
<DIAGCODES>
  <DIAGCODE>
    <DIAGCODE.code_id V="250.00"/>
    <DIAGCODE.code_desc V="Diabetes mellitus type II"/>
  </DIAGCODE>
</DIAGCODES>
```
- `code_id` 最长 17 字符，`code_desc` 最长 255 字符。

---

## 五、第三方接口特殊规则

### 5.1 结果接口（NovaNet → LIS/HIS）

- NovaNet **模拟设备**向第三方发起会话，每批结果一个新会话
- 设备位置通过 `DEV.device_name` 文本节点传递：`facility^location`
- 患者位置通过 `PT.location` 传递：`facility^location^room^bed`
- 注释携带 `CH` 和 `FL` 标志
- 会话在 Basic Profile 流程结束后终止

### 5.2 操作员列表接口（第三方 → NovaNet）

- 单一会话，进入连续模式后保持连接
- 启动时发送完整列表，后续发送增量列表
- 每条操作员记录必须包含 `facility^location^inst_type`
- 支持通配符 `ALL`（facility=ALL 时 location 也必须为 ALL）
- `I` 动作同时表示插入和更新
- 同一操作员在同一设备类型的所有位置必须有相同的权限级别和认证方法

---

## 六、手动测试结果传输

手动测试结果使用 `OBS.R01`（患者）或 `OBS.R02`（QC）消息传输，支持三种数据类型：

| 类型 | 使用字段 | 说明 |
|------|----------|------|
| **数值型** | `OBS.value` | 含数值和单位，配合 `interpretation_cd` |
| **选择列表型** | `OBS.qualitative_value` | 从预定义列表中选择 |
| **文本型** | `OBS.qualitative_value` | 用户自由输入文本 |

多子测试面板通过 `ORD.universal_service_id` 传递面板名称，每个子测试作为一个独立的 `OBS` 对象。

---

以上是基于 DML_Novanet Interface Specs v2.59 文档的完整中文解析。该协议基于 POCT1-A 标准，采用 XML 编码和同步确认（SA）连接模式，覆盖了从设备连接、数据上传、列表同步到固件升级的完整生命周期管理。如需进一步了解特定消息或字段的细节，请告知。