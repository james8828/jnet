我注意到您想要根据 `novanet.sql` 文件生成中文数据库字典和表业务关系图。让我先查看这个SQL文件的内容。


用户需要根据SQL文件生成数据库字典和业务关系图,我需要先读取该SQL文件的内容来了解其结构。

用户要求根据novanet.sql文件生成中文数据库字典和表业务关系图。我已经读取了完整的SQL文件内容，现在需要分析所有表结构、字段、注释和关系，然后生成：
1. 中文数据库字典 - 包含所有表的中文说明
2. 表业务关系图 - 展示表之间的关联关系

让我分析这个数据库的结构：

这是一个医疗仪器管理系统（Nova Bio医疗诊断设备）的数据库，主要功能包括：
- 仪器通信管理
- 患者信息管理
- 操作员管理
- 样本/检测结果管理
- 质控管理
- 位置/机构管理
- 系统配置管理

我需要将这些信息整理成中文文档格式。由于这是一个复杂的数据库，我应该创建一个结构化的文档来展示这些信息。

根据SQL文件内容，我来为您生成中文数据库字典和表业务关系图。

## 📊 NovaNet 数据库字典

### 一、数据库概述
这是一个**医疗诊断仪器管理系统**数据库，主要用于管理Nova Bio医疗设备的通信、患者信息、检测结果、质控等核心业务。

---

### 二、表分类及详细说明

#### 📡 1. 通信管理模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `COMM_AUDIT` | 通信审计日志 | (Computer_Name, Instrument_ID, Event_DateTime, Event_Code) | 记录仪器通信事件，如结果接收、设备事件、列表发送等 |
| `Communications` | 通信配置 | comm_record_num | 仪器通信参数配置，包括协议、端口、波特率、IP等 |
| `health_ping` | 健康监控 | (process_name, host) | 非SOA组件(如ICP、MGR)的心跳监控报告 |

**通信事件代码说明：**
- `1` = 接收仪器结果
- `2` = 接收设备事件
- `3` = 发送操作员列表
- `4` = 发送患者列表
- `5` = 发送位置列表
- `6` = 发送医生列表
- `7` = 发送试剂信息
- `8` = 发送设置
- `9` = 发送固件更新
- `10` = 连接
- `11` = 断开连接

---

#### 🏥 2. 患者管理模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `PATIENTS` | 患者基本信息 | patient_uuid | 患者档案，包含姓名、性别、生日、病历号等 |
| `PATIENT_ACCOUNTS` | 患者账户 | account_uuid | 患者账户信息，与患者一对多关系 |
| `PATIENT_VISITS` | 患者就诊记录 | visit_uuid | 就诊/住院记录，包含入院时间、病房、医生等 |
| `PATIENT_INCREMENTAL_D` | 患者增量数据 | row_num | 患者数据增量更新记录 |
| `ORDERS` | 医嘱/检验申请 | order_key_num | 检验申请单，关联患者、样本类型、检测项目 |

**患者标识体系：**
- `Patient_ID`: 企业级患者ID
- `medrec_num`: 病历号
- `account_num`: 账户号
- `visit_num`: 就诊号
- `Accession_num`: 检验申请号

---

#### 🔬 3. 样本与检测模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `SAMPLES` | 样本/检测结果 | sample_key_num | 样本检测结果，包含XML结果数据、质控类型等 |
| `SAMPLE_TYPES` | 样本类型 | sample_type_name | 样本类型定义(如血液、尿液等) |
| `INSTRUMENTS_TESTS` | 仪器检测项目 | instruments_tests_num | 仪器可执行的检测项目配置 |
| `Test_Offsets` | 检测偏移值 | (generic_test_name, units, inst_type, loc_num) | 检测项目的校准偏移值 |
| `test_range` | 检测参考范围 | 无独立主键 | 检测项目的正常范围、危急值范围，按性别年龄分组 |
| `test_comment` | 检测注释 | comment_num | 检测项目的注释/备注定义 |
| `test_comment_to_loc` | 注释位置关联 | (comment_num, loc_num) | 检测注释与位置的关联 |

**样本控制类型：**
- `UNK` = 未知
- `OBS` = 观察(患者结果)
- `PRF` = 能力验证
- `CVR` = 校准验证(线性)
- `EQC` = 电子质控
- `LQC` = 液体质控

---

#### 👨‍⚕️ 4. 操作员管理模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `OPERATORS` | 操作员信息 | operator_num | 操作员档案，包含ID、主管关系等 |
| `operator_privilege` | 操作员权限 | (operator_num, inst_type, test_name) | 操作员对仪器和检测项目的权限、密码、认证有效期 |
| `operator_to_unit` | 操作员位置关联 | (operator_num, loc_num) | 操作员与使用位置的关联 |
| `operator_message` | 操作员消息 | (operator_num, msg_create_dttm) | 发送给操作员的消息通知 |
| `EXT_USER` | 外部用户 | user_name | 外部用户认证信息 |

**权限等级：**
- `1` = 主管
- `4` = 用户
- `5` = 服务
- `6` = 培训

---

#### 🏭 5. 仪器与设备模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `INSTRUMENT_TYPES` | 仪器类型 | inst_type | 仪器类型定义，如分析仪、ADT、LIS等 |
| `instruments` | 仪器设备 | inst_num | 具体仪器实例，包含序列号、位置、状态等 |
| `device_events` | 设备事件 | uuid | 设备维护、错误等事件记录 |
| `bga_setup_to_location` | BGA设置位置关联 | (inst_type, loc_num) | BGA(血气分析)仪器与位置的设置关联 |
| `bga_cartridge_status` | BGA试剂筒状态 | (cartridge_type, inst_num) | BGA试剂筒的剩余量和状态 |
| `bga_state` | BGA仪器状态 | inst_num | BGA仪器的主要状态和详细信息 |
| `bga_test_status` | BGA检测状态 | (inst_num, observation_id) | BGA检测项目的校准和质控状态 |

**仪器状态：**
- `L` = 质控锁定
- `P` = 部分质控锁定
- `R` = 就绪
- `B` = 忙碌
- `S` = 待机

---

#### 🧪 6. 批号与质控模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `LOTS` | 批号主表 | lots_key_num | 试剂/质控品批号，包含批号、有效期、使用状态等 |
| `LOT_CHEM` | 批号化学参数 | (lots_key_num, generic_test_name, Units, facility_num) | 批号的检测参数，包含靶值、标准差、Westgard质控规则 |
| `device_to_lot` | 设备批号关联 | (lots_key_num, inst_type) | 设备支持的批号关联 |
| `lots_to_unit` | 批号位置关联 | (lots_key_num, loc_num) | 批号与使用位置的关联 |
| `lot_chem_defaults` | 批号化学默认值 | (generic_test_name, lot_level, lot_type, units_of_measure, lot_num_id) | 批号化学参数的默认高低范围 |
| `lot_insert_ranges` | 批号插入范围 | (lots_key_num, units_of_measure, generic_test_name) | 批号检测项目的范围值 |

**批号类型：**
- `Control` = 质控品
- `Reagent` = 试剂
- `Linearity` = 线性
- `TestStrip` = 试纸条

**Westgard质控规则：**
- `2-2s`: 连续2个样本超过2个标准差
- `4-1s`: 连续4个样本超过1个标准差
- `10x`: 连续10个样本在均值同一侧

---

#### 📍 7. 位置与机构模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `inst_locations` | 位置/科室 | loc_num | 层级位置结构(机构-科室-病房等) |
| `facility_patient_test_rails` | 机构检测范围 | (facility_id, test_name, units) | 机构级别的检测项目参考范围 |
| `facility_prefs` | 机构偏好设置 | facility_uuid | 机构的数据格式掩码、时区、LIS馈送设置等 |
| `facility_setup_prefs` | 机构检测单位设置 | 无主键 | 机构默认检测单位(肌酐、酮体、葡萄糖等) |
| `facility_ht_wt_units` | 机构身高体重单位 | loc_num | 机构的身高体重单位制 |
| `facility_test_units` | 机构检测单位 | (generic_test_name, units_of_measure, loc_num) | 机构检测项目的单位配置 |
| `loc_def_pat_id` | 位置默认患者ID | loc_num | 位置默认要求的患者ID类型 |
| `loc_last_update` | 位置最后更新 | (loc_num, data_type, inst_type) | 位置数据下载的最后更新时间 |
| `loc_to_config` | 位置配置关联 | (loc_num, inst_type, config_num) | 位置与仪器配置的关联 |
| `loc_to_firmware` | 位置固件关联 | (loc_num, firmware_num) | 位置与固件版本的关联 |
| `loc_to_panel` | 位置面板关联 | 无主键 | 位置与检测面板的关联 |
| `loc_to_wifi_setup` | 位置WiFi设置 | 无主键 | 位置WiFi配置 |
| `location_alias` | 位置别名 | (loc_id, alias) | 位置的别名定义 |
| `auto_discharge_fac` | 机构自动出院 | (loc_num, class_type, adt_id) | 机构级别的自动出院规则 |
| `auto_discharge_loc` | 位置自动出院 | (parent_uuid, loc_num, adt_id) | 位置级别的自动出院规则 |

---

#### 👨‍⚕️ 8. 医生管理模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `PHYSICIANS` | 医生信息 | Physician_ID | 医生档案 |
| `physician_to_unit` | 医生位置关联 | (physician_id, loc_num) | 医生与使用位置的关联 |

---

#### 💾 9. 固件与配置模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `DownloadFirmware` | 固件下载数据 | (inst_type, firmware_version, index_num) | 固件数据块，用于仪器固件更新 |
| `FIRMWARE` | 固件信息 | firmware_num | 固件文件路径、版本等元数据 |
| `config_data` | 配置数据 | (config_num, directive_name, _key) | 系统配置参数 |
| `loc_to_config` | 位置配置关联 | (loc_num, inst_type, config_num) | 位置与配置的关联 |

---

#### 📋 10. 诊断与面板模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `diagnosis_codes` | 诊断代码 | (diagnosis_code, generic_test_name) | 诊断代码定义 |
| `diagnosis_to_unit` | 诊断位置关联 | (diagnosis_code, loc_num, generic_test_name) | 诊断代码与位置的关联 |
| `manual_tests` | 手动检测项目 | mt_num | 手动检测项目定义，包含XML数据 |
| `mt_lot_to_loc` | 手动检测批号位置 | 无主键 | 手动检测批号与位置关联 |
| `loc_to_panel` | 位置检测面板 | 无主键 | 位置与检测面板关联 |

---

#### ⚙️ 11. 系统管理模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `ErrorMessages` | 错误消息 | err_msg_num | 系统错误日志，包含严重程度、处理状态等 |
| `arch_export_backup` | 归档导出备份 | (data_type, arch_export) | 数据归档、导出、备份配置 |
| `maint_plan` | 维护计划 | plan_id | 数据库维护计划，包含验证、备份、邮件通知等 |
| `maint_plan_report` | 维护计划报告 | (plan_id, start_time) | 维护计划执行报告 |
| `service_packs` | 服务包 | name | 系统服务包安装记录 |
| `component_update` | 组件更新 | cu_uuid | 组件更新执行记录 |
| `version_info` | 版本信息 | object_name | 系统对象版本信息 |
| `authorized_services` | 授权服务 | (service_name, pop_name) | 授权启动的服务 |
| `pop_info` | POP服务器信息 | 无主键 | POP服务器URL和SSL状态 |
| `process_control` | 进程控制 | (pc_process, pc_key, loc_uuid, fac_uuid) | 进程控制参数 |
| `time_zone` | 时区 | zone_name | 时区定义 |
| `contact_info` | 联系信息 | contact_num | 联系人信息，可关联多个表 |

---

#### 🖥️ 12. UI用户界面模块

| 表名 | 中文名称 | 主键 | 业务说明 |
|------|---------|------|---------|
| `ui_user` | UI用户 | uid | 系统用户账户 |
| `ui_user_to_domain` | 用户域关联 | (user_id, domain_id) | 用户与域的关联 |
| `ui_user_to_facility` | 用户机构关联 | 无主键 | 用户与机构的关联 |
| `ui_password_properties` | 密码属性 | 无主键 | 密码策略配置 |
| `ui_old_passwords` | 旧密码 | 无主键 | 历史密码记录 |
| `ui_menu_entry` | 菜单项 | (major, minor) | UI菜单配置 |
| `ui_general` | UI通用设置 | 无主键 | 语言、登录转发等通用设置 |
| `ui_instruments` | UI仪器显示 | name | UI中显示的仪器配置 |
| `ui_comm_ports_page_control` | 通信端口页面控制 | record_id | 通信端口页面控件配置 |
| `ui_sticky_attributes` | 用户粘性属性 | (user_name, attrib_page, attrib_name) | 用户界面偏好设置 |
| `user_alive` | 用户在线状态 | user_name | 用户在线心跳监控 |
| `recertification_messages` | 重新认证消息 | facility_id | 操作员重新认证提醒消息 |

---

### 三、核心业务关系图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        NovaNet 系统架构                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   医疗仪器       │◄─────►│  通信管理层      │◄─────►│   数据接收      │
│  (instruments)  │       │ (Communications)│       │  (SAMPLES)     │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  仪器类型       │       │  通信审计        │       │  检测结果       │
│(INSTRUMENT_TYPES)│      │  (COMM_AUDIT)   │       │  (SAMPLES)     │
└────────┬────────┘       └─────────────────┘       └────────┬────────┘
         │                                                    │
         ▼                                                    ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  检测项目       │       │  错误日志        │       │  患者信息       │
│(INSTRUMENTS_  │◄─────►│ (ErrorMessages) │       │  (PATIENTS)    │
│    TESTS)      │       └─────────────────┘       └────────┬────────┘
└────────┬────────┘                                         │
         │                                                  ▼
         ▼                                         ┌─────────────────┐
┌─────────────────┐                                │  患者账户       │
│  参考范围       │                                │(PATIENT_       │
│  (test_range)   │                                │ ACCOUNTS)      │
└────────┬────────┘                                └────────┬────────┘
         │                                                  │
         │                                                  ▼
         │                                         ┌─────────────────┐
         │                                         │  就诊记录       │
         │                                         │(PATIENT_       │
         │                                         │  VISITS)       │
         │                                         └─────────────────┘
         │
┌────────┴────────┐       ┌─────────────────┐       ┌─────────────────┐
│  质控批号       │◄─────►│  批号化学参数   │◄─────►│   位置/机构     │
│    (LOTS)       │       │   (LOT_CHEM)    │       │(inst_locations) │
└────────┬────────┘       └─────────────────┘       └────────┬────────┘
         │                                                    │
         ▼                                                    ▼
┌─────────────────┐                                ┌─────────────────┐
│  设备批号关联   │                                │  操作员         │
│(device_to_lot) │                                │(OPERATORS)     │
└─────────────────┘                                └────────┬────────┘
                                                            │
                                                            ▼
                                                   ┌─────────────────┐
                                                   │  操作员权限     │
                                                   │(operator_      │
                                                   │ privilege)     │
                                                   └─────────────────┘
```

---

### 四、核心业务流程

#### 1. 检测流程
```
仪器采集样本 → 通信传输 → 结果接收(SAMPLES) → 关联患者 → 质控验证 → 报告生成
```

#### 2. 质控流程
```
批号管理(LOTS) → 化学参数(LOT_CHEM) → Westgard规则验证 → 质控状态更新
```

#### 3. 操作员管理流程
```
操作员注册 → 权限分配 → 认证有效期管理 → 消息通知 → 使用审计
```

#### 4. 数据管理流程
```
数据积累 → 定期归档 → 备份验证 → 维护计划执行
```

---

### 五、关键外键关系

| 主表 | 外键表 | 关联字段 | 关系说明 |
|------|--------|---------|---------|
| PATIENTS | PATIENT_ACCOUNTS | patient_uuid | 一对多 |
| PATIENTS | PATIENT_VISITS | patient_uuid | 一对多 |
| PATIENT_ACCOUNTS | PATIENT_VISITS | account_uuid | 一对多 |
| INSTRUMENT_TYPES | instruments | inst_type | 一对多 |
| INSTRUMENT_TYPES | INSTRUMENTS_TESTS | inst_type | 一对多 |
| LOTS | LOT_CHEM | lots_key_num | 一对多 |
| LOTS | lots_to_unit | lots_key_num | 一对多 |
| OPERATORS | operator_privilege | operator_num | 一对多 |
| OPERATORS | operator_to_unit | operator_num | 一对多 |
| inst_locations | instruments | loc_num | 一对多 |
| inst_locations | lots_to_unit | loc_num | 一对多 |
| instruments | device_events | inst_num | 一对多 |
| instruments | bga_state | inst_num | 一对一 |
| maint_plan | maint_plan_report | plan_id | 一对多 |

---

这个数据库设计体现了**医疗检验信息系统**的典型架构，核心围绕**仪器-样本-患者-质控**四大业务主线，通过位置/机构层级实现多站点管理。