-- ============================================================
-- NovaNet RTM 基础表结构
-- 对应 C# 源码: RTMADTP, RTMOPL, RTMLIS
-- ============================================================

-- -----------------------------------------------------------
-- 1. 患者表 (Patient)
--    来源: RTMADTP/DBPatient
--    存储患者基本信息, 关联 account 和 visit
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_num         VARCHAR(40)  NOT NULL,           -- 内部患者主键 UUID
    patient_id          VARCHAR(40),                     -- 外部患者ID (PID-2)
    medrec_num          VARCHAR(40),                     -- 病历号 MRN (PID-3)
    last_name           VARCHAR(80),                     -- 姓 (PID-5.1)
    first_name          VARCHAR(80),                     -- 名 (PID-5.2)
    middle_name         VARCHAR(80),                     -- 中间名 (PID-5.3)
    prefix              VARCHAR(40),                     -- 前缀 (PID-5.4)
    suffix              VARCHAR(40),                     -- 后缀 (PID-5.5)
    birth_date          DATE,                            -- 出生日期 (PID-7)
    sex                 VARCHAR(1),                      -- 性别 (PID-8): M/F/U
    race                VARCHAR(40),                     -- 种族 (PID-9)
    address             VARCHAR(255),                    -- 地址 (PID-11)
    phone_home          VARCHAR(40),                     -- 电话 (PID-13)
    active              CHAR(1) DEFAULT 'T',             -- 活跃标志 T/F
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (patient_num),
    INDEX idx_patient_medrec (medrec_num),
    INDEX idx_patient_patient_id (patient_id)
);

-- -----------------------------------------------------------
-- 2. 患者账户表 (Patient Account)
--    来源: RTMADTP/DBPatientAccount
--    一个患者可有多个账户
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_account (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_num         VARCHAR(40)  NOT NULL,           -- 账户主键 UUID
    patient_num         VARCHAR(40)  NOT NULL,           -- 关联患者
    account_number      VARCHAR(40),                     -- 外部账号 (PID-18)
    account_facility    VARCHAR(40),                     -- 账户机构
    active              CHAR(1) DEFAULT 'T',
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_num),
    INDEX idx_pa_patient (patient_num),
    INDEX idx_pa_account_number (account_number)
);

-- -----------------------------------------------------------
-- 3. 患者就诊表 (Patient Visit)
--    来源: RTMADTP/DBPatientVisit
--    一个账户可有多个就诊记录
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_visit (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_num           VARCHAR(40)  NOT NULL,           -- 就诊主键 UUID
    account_num         VARCHAR(40),                     -- 关联账户
    patient_num         VARCHAR(40)  NOT NULL,           -- 关联患者
    visit_number        VARCHAR(40),                     -- 外部就诊号 (PV1-19)
    patient_class       VARCHAR(1),                      -- 患者类别 (PV1-2): I/O/E
    location            VARCHAR(80),                     -- 科室 (PV1-3.1)
    room                VARCHAR(40),                     -- 房间 (PV1-3.2)
    bed                 VARCHAR(40),                     -- 床位 (PV1-3.3)
    facility            VARCHAR(80),                     -- 机构 (PV1-3.4)
    prior_location      VARCHAR(80),                     -- 前一科室 (PV1-6)
    attending_physician VARCHAR(80),                     -- 主治医生 (PV1-7)
    hospital_service    VARCHAR(40),                     -- 医院服务 (PV1-10)
    patient_type        VARCHAR(40),                     -- 患者类型 (PV1-18)
    admit_date          TIMESTAMP,                       -- 入院时间 (PV1-44)
    discharge_date      TIMESTAMP,                       -- 出院时间 (PV1-45)
    active              CHAR(1) DEFAULT 'T',
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (visit_num),
    INDEX idx_pv_account (account_num),
    INDEX idx_pv_patient (patient_num),
    INDEX idx_pv_visit_number (visit_number)
);

-- -----------------------------------------------------------
-- 4. 医院组织机构表 (Location / Facility)
--    来源: RTMADTP/FacilityList, LocationList
--    支持多层级机构 (机构 > 科室)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS inst_locations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num             VARCHAR(40)  NOT NULL,           -- 位置编号 UUID
    loc_name            VARCHAR(100),                    -- 位置名称
    level_num           INT DEFAULT 0,                   -- 层级: 0=根机构, 1=科室
    parent              VARCHAR(40),                     -- 父级 loc_num (机构为NULL)
    loc_alias           VARCHAR(100),                    -- 位置别名
    inst_class          VARCHAR(40),                     -- 仪器类别 (关联 DML 协议)
    inst_type           VARCHAR(40),                     -- 仪器类型 (关联 DML 协议)
    active              CHAR(1) DEFAULT 'T',
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (loc_num),
    INDEX idx_il_parent (parent),
    INDEX idx_il_loc_name (loc_name)
);

-- -----------------------------------------------------------
-- 5. 医护人员表 (Operator)
--    来源: RTMOPL/DBOperator
--    存储操作员/医护人员基本信息
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS operators (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num        VARCHAR(40)  NOT NULL,           -- 操作员主键 UUID
    supervisor_num      VARCHAR(40),                     -- 上级操作员
    operator_id         VARCHAR(80)  NOT NULL,           -- 操作员ID (登录名)
    is_supervisor       VARCHAR(1),                      -- 是否主管 T/F
    last_name           VARCHAR(80),                     -- 姓
    first_name          VARCHAR(80),                     -- 名
    initials            VARCHAR(20),                     -- 首字母
    email               VARCHAR(100),                    -- 邮箱
    locked_by           VARCHAR(40),                     -- 锁定者
    active              CHAR(1) DEFAULT 'T',
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (operator_num),
    INDEX idx_op_operator_id (operator_id)
);

-- -----------------------------------------------------------
-- 6. 操作员权限表 (Operator Privilege)
--    来源: RTMOPL/DBOperatorPrivilege
--    操作员按仪器类型/项目的权限
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS operator_privilege (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num        VARCHAR(40)  NOT NULL,           -- 关联操作员
    inst_type           VARCHAR(40)  NOT NULL,           -- 仪器类型
    test_name           VARCHAR(80),                     -- 测试项目
    pswd                VARCHAR(100),                    -- 密码(加密)
    cert_start_date     DATE,                            -- 认证开始日期
    cert_end_date       DATE,                            -- 认证结束日期
    privilege           INT DEFAULT 0,                   -- 权限级别: 1=管理员, 2=技术员, 4=普通
    is_active           CHAR(1) DEFAULT 'T',
    is_active_last_update_date TIMESTAMP,
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (operator_num, inst_type, test_name),
    INDEX idx_op_priv_op (operator_num)
);

-- -----------------------------------------------------------
-- 7. 操作员科室关联表 (Operator To Unit)
--    来源: RTMOPL/DBOperatorToUnit
--    操作员可在多个科室/机构工作
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS operator_to_unit (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num        VARCHAR(40)  NOT NULL,           -- 关联操作员
    loc_num             VARCHAR(40)  NOT NULL,           -- 关联科室
    is_active           CHAR(1) DEFAULT 'T',
    is_active_last_update_date TIMESTAMP,
    UNIQUE (operator_num, loc_num),
    INDEX idx_otu_loc (loc_num)
);

-- -----------------------------------------------------------
-- 8. 操作员方法/项目表 (Operator Methods)
--    来源: RTMOPL/DBMethod
--    操作员授权的检测项目
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS methods (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num        VARCHAR(40)  NOT NULL,
    inst_type           VARCHAR(40)  NOT NULL,
    method_cd           VARCHAR(80)  NOT NULL,            -- 方法/项目代码
    UNIQUE (operator_num, inst_type, method_cd),
    INDEX idx_m_inst_type (inst_type)
);

-- -----------------------------------------------------------
-- 9. 样本/检测结果表 (Samples / Test Results)
--    来源: RTMLIS, DML 协议 OBS.R01/OBS.R02
--    存储从设备接收的检测结果 (含 XML 原始数据)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS samples (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_key_num      VARCHAR(40)  NOT NULL,           -- 样本主键 UUID
    sample_date         TIMESTAMP,                       -- 采样时间
    device_name         VARCHAR(100),                    -- 仪器名称
    device_type         VARCHAR(40),                     -- 仪器类型
    device_serial       VARCHAR(40),                     -- 仪器序列号
    device_sw_ver       VARCHAR(40),                     -- 仪器软件版本
    loc_name            VARCHAR(100),                    -- 科室名称
    fac_name            VARCHAR(100),                    -- 机构名称
    control_type        VARCHAR(2),                      -- 控制类型: OBS=患者结果, SVC=质控
    transmitted_flag    CHAR(1) DEFAULT 'F',             -- 发送标志 T/F
    xml_text            TEXT,                            -- DML XML 原始数据 (含<SVC>/<OBS>)
    patient_num         VARCHAR(40),                     -- 关联患者 (如果有)
    visit_num           VARCHAR(40),                     -- 关联就诊 (如果有)
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    add_date            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sample_key_num),
    INDEX idx_samp_transmit (transmitted_flag),
    INDEX idx_samp_patient (patient_num),
    INDEX idx_samp_device (device_serial)
);

-- -----------------------------------------------------------
-- 10. 观测结果表 (Observation Results)
--     来源: RTMLIS, DML 协议 OBS 节点
--     存储从样本 XML 中解析的具体观测值
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS observation_result (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_key_num      VARCHAR(40)  NOT NULL,           -- 关联样本
    test_code           VARCHAR(80),                     -- 测试代码
    test_name           VARCHAR(100),                    -- 测试名称
    value               VARCHAR(255),                     -- 结果值
    units               VARCHAR(40),                     -- 单位
    value_type          VARCHAR(2),                       -- 值类型: NM/ST
    reference_range     VARCHAR(100),                    -- 参考范围
    abnormal_flags      VARCHAR(10),                     -- 异常标志: H/L/A
    result_status       VARCHAR(2),                      -- 结果状态: F/P/V/W
    operator_id         VARCHAR(80),                     -- 操作员ID
    observation_date    TIMESTAMP,                       -- 观测时间
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_or_sample (sample_key_num),
    INDEX idx_or_test_code (test_code)
);

-- -----------------------------------------------------------
-- 11. 质控结果表 (QC Results)
--     来源: RTMLIS, DML 协议 SVC 节点
--     存储质控数据 (含质控物信息)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS qc_result (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_key_num      VARCHAR(40)  NOT NULL,           -- 关联样本
    control_type        VARCHAR(40),                     -- 质控类型
    lot_number          VARCHAR(40),                     -- 批号
    lot_level           VARCHAR(40),                     -- 质控水平
    manufacturer_id     VARCHAR(40),                     -- 厂家ID
    container_id        VARCHAR(40),                     -- 容器ID
    specimen_source     VARCHAR(40),                     -- 样本来源
    qc_date             TIMESTAMP,                       -- 质控时间
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_qr_sample (sample_key_num),
    INDEX idx_qr_lot (lot_number)
);

-- -----------------------------------------------------------
-- 12. 位置最后更新表 (Location Last Update)
--     来源: RTMOPL/DBLocLastUpdate
--     记录各位置数据最后更新时间 (用于增量同步)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS loc_last_update (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num             VARCHAR(40)  NOT NULL,
    data_type           VARCHAR(40)  NOT NULL,           -- 数据类型: OPERATORS, PATIENTS
    inst_class          VARCHAR(40),                     -- 仪器类别
    inst_type           VARCHAR(40),                     -- 仪器类型
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (loc_num, data_type, inst_type)
);

-- -----------------------------------------------------------
-- 13. 通信配置表 (Communications)
--     来源: RTMADTP/RTMOPL/RTMLIS 初始化
--     存储各服务的端口/协议/连接参数
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS communications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    instrument_id       VARCHAR(40),
    protocol            VARCHAR(40),                     -- 协议类型: DML, ASTM, HL7
    port_type           VARCHAR(20),                     -- 端口类型: ADT, OPL, LIS
    comm_protocol       VARCHAR(20),                     -- 通信协议: TCP, SERIAL
    port_num            INT,
    baud                VARCHAR(20),
    data_bits           VARCHAR(2),
    stop_bits           VARCHAR(2),
    parity              VARCHAR(2),
    flow_control        INT DEFAULT 0,
    run_mode            INT DEFAULT 0,
    connect_remote      INT DEFAULT 0,
    used                CHAR(1) DEFAULT 'T',
    multi_connect       VARCHAR(40),
    ip_address          VARCHAR(40),
    rcv_application     VARCHAR(40),
    rcv_facility        VARCHAR(40),
    port_active         INT DEFAULT 1,
    remote_host_name    VARCHAR(100),
    remote_port         INT,
    computer_name       VARCHAR(100),
    from_ui             CHAR(1) DEFAULT 'T',
    last_update_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------
-- 14. 健康状态表 (Health Ping)
--     来源: 各 RTM 服务 UpdateHealthPing()
--     记录服务运行状态
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_ping (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_name        VARCHAR(40)  NOT NULL,           -- RTMADTP, RTMOPL, RTMLIS
    host                VARCHAR(100)  NOT NULL,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_connect_dttm   TIMESTAMP,
    last_disconnect_dttm TIMESTAMP,
    tot_messages_processed INT DEFAULT 0,
    UNIQUE (process_name, host)
);