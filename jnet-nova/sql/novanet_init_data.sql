
-- ============================================================================
-- NovaNet 初始化数据 (Sybase SQL Anywhere 语法)
-- 基于 novanet.sql 表结构, 梳理 POCT 完整业务链路
-- ============================================================================
-- 业务域:
--   组织机构 → 仪器类型/检测项 → 医护人员(权限/科室/方法)
--            → 医生 → 患者(账户/就诊) → 试剂批号(化学范围) → 仪器/通信
-- 插入顺序严格遵循外键依赖:
--   time_zone → INSTRUMENT_TYPES → inst_locations → SAMPLE_TYPES
--   → config_data/loc_to_config → facility_prefs/facility_ht_wt_units/loc_def_pat_id
--   → PHYSICIANS/physician_to_unit → OPERATORS/operator_privilege/operator_to_unit
--   → INSTRUMENTS_TESTS → instruments → Communications
--   → LOTS/device_to_lot/lots_to_unit/LOT_CHEM
--   → PATIENTS/PATIENT_ACCOUNTS/PATIENT_VISITS → health_ping
-- ============================================================================
SET TEMPORARY OPTION CONNECTION_AUTHENTICATION='Company=Nova Biomedical;Application=NovaNet;Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158'
-- ----------------------------------------------------------------------------
-- 1. 时区定义 (time_zone)
-- ----------------------------------------------------------------------------
INSERT INTO time_zone (zone_name, zone_offset) VALUES ('UTC', '+00:00');
INSERT INTO time_zone (zone_name, zone_offset) VALUES ('China Standard Time', '+08:00');
INSERT INTO time_zone (zone_name, zone_offset) VALUES ('US Eastern', '-05:00');

-- ----------------------------------------------------------------------------
-- 2. 仪器类型 (INSTRUMENT_TYPES)
--    inst_class: Analyzer=分析仪, ADT=患者管理, LIS=实验室信息系统
-- ----------------------------------------------------------------------------
INSERT INTO INSTRUMENT_TYPES (inst_type, does_remote_review, inst_class, use_inst_lot_data)
VALUES ('StatStrip', 'F', 'Analyzer', 'T');

-- ----------------------------------------------------------------------------
-- 3. 组织机构层级 (inst_locations)
--    level_num: 1=机构(Facility), 2=科室(Unit); parent='0' 表示根节点
-- ----------------------------------------------------------------------------
-- 机构: 中心医院
INSERT INTO inst_locations (loc_num, parent, level_num, loc_name, is_default, restrict_to_local_queries)
VALUES ('a1000000-0000-0000-0000-000000000001', '0', 1, '中心医院', 'T', 'F');
-- 科室: ICU 重症监护室
INSERT INTO inst_locations (loc_num, parent, level_num, loc_name, is_default, restrict_to_local_queries)
VALUES ('a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001', 2, 'ICU重症监护室', 'T', 'F');
-- 科室: 急诊科
INSERT INTO inst_locations (loc_num, parent, level_num, loc_name, is_default, restrict_to_local_queries)
VALUES ('a1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001', 2, '急诊科', 'F', 'F');
-- 科室: 普外科
INSERT INTO inst_locations (loc_num, parent, level_num, loc_name, is_default, restrict_to_local_queries)
VALUES ('a1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000001', 2, '普外科', 'F', 'F');
-- 科室: 内科
INSERT INTO inst_locations (loc_num, parent, level_num, loc_name, is_default, restrict_to_local_queries)
VALUES ('a1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000001', 2, '内科', 'F', 'F');

-- ----------------------------------------------------------------------------
-- 4. 样本类型 (SAMPLE_TYPES)
--    sample_type_code: LOINC 编码
-- ----------------------------------------------------------------------------
INSERT INTO SAMPLE_TYPES (sample_type_code, sample_type_name, sample_type_transmit_name)
VALUES ('11779-6', 'Whole Blood', 'Whole Blood');
INSERT INTO SAMPLE_TYPES (sample_type_code, sample_type_name, sample_type_transmit_name)
VALUES ('42933-7', 'Plasma', 'Plasma');
INSERT INTO SAMPLE_TYPES (sample_type_code, sample_type_name, sample_type_transmit_name)
VALUES ('12710-1', 'Serum', 'Serum');

-- ----------------------------------------------------------------------------
-- 5. 系统配置 (config_data)
--    PK (config_num, directive_name, "_key"); "_key" 含 *V 后缀为通用值
-- ----------------------------------------------------------------------------
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG001', 'patient_id', 'max_length*V', '20');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG002', 'patient_id', 'min_length*V', '1');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG003', 'operator_id', 'max_length*V', '20');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG004', 'operator_id', 'min_length*V', '1');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG005', 'accn_id', 'max_length*V', '16');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG006', 'accn_id', 'min_length*V', '1');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG007', 'mrn', 'max_length*V', '20');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG008', 'mrn', 'min_length*V', '1');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG009', 'global', 'allow_testing_default_unit*V', '1');
INSERT INTO config_data (config_num, directive_name, "_key", "_value")
VALUES ('CFG010', 'global', 'obs_review_no_login*V', '1');

-- ----------------------------------------------------------------------------
-- 6. 科室配置关联 (loc_to_config)
--    PK (loc_num, inst_type, config_num); 将配置绑定到科室+仪器类型
-- ----------------------------------------------------------------------------
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000002', 'StatStrip', 'CFG001', 'ICU患者ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000002', 'StatStrip', 'CFG003', 'ICU操作员ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000002', 'StatStrip', 'CFG005', 'ICU accession配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000002', 'StatStrip', 'CFG009', 'ICU全局配置', 'T');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000003', 'StatStrip', 'CFG001', '急诊患者ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000003', 'StatStrip', 'CFG003', '急诊操作员ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000003', 'StatStrip', 'CFG009', '急诊全局配置', 'T');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000004', 'StatStrip', 'CFG001', '普外患者ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000004', 'StatStrip', 'CFG009', '普外全局配置', 'T');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000005', 'StatStrip', 'CFG001', '内科患者ID配置', 'F');
INSERT INTO loc_to_config (loc_num, inst_type, config_num, config_name, is_global)
VALUES ('a1000000-0000-0000-0000-000000000005', 'StatStrip', 'CFG009', '内科全局配置', 'T');

-- ----------------------------------------------------------------------------
-- 7. 机构偏好 (facility_prefs)
--    定义各 ID 字段长度/掩码、LIS feed 等机构级设置
-- ----------------------------------------------------------------------------
INSERT INTO facility_prefs (
    facility_uuid, PatIdMinLength, PatIdMaxLength, PatId1DMask, PatId1DMaskLong, PatId2DMask,
    MrnMinLength, MrnMaxLength, Mrn1DMask, Mrn1DMaskLong, Mrn2DMask,
    AcctNumMinLength, AcctNumMaxLength, AcctNum1DMask, AcctNum1DMaskLong, AcctNum2DMask,
    AccnIdMinLength, AccnIdMaxLength, AccnId1DMask, AccnId1DMaskLong, AccnId2DMask,
    lis_feed_state_has_been_validated, time_zone, date_format, lis_feed_active, time_format
) VALUES (
    'a1000000-0000-0000-0000-000000000001',
    '1', '20', 'XXXXXXXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXXXXXXX', 'XXXXX-XXXXX-XXXXX-XXXXX',
    '1', '20', 'XXXXXXXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXXXXXXX', 'XXXXX-XXXXX-XXXXX-XXXXX',
    '1', '16', 'XXXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXXX', 'XXXX-XXXX-XXXX-XXXX',
    '1', '16', 'XXXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXXX', 'XXXX-XXXX-XXXX-XXXX',
    'T', 'China Standard Time', 'yyyy-MM-dd', 'T', 'HH:mm:ss'
);

-- ----------------------------------------------------------------------------
-- 8. 机构身高体重单位 (facility_ht_wt_units)
--    loc_num 引用 inst_locations(机构层级)
-- ----------------------------------------------------------------------------
INSERT INTO facility_ht_wt_units (loc_num, weight_units, height_units)
VALUES ('a1000000-0000-0000-0000-000000000001', 'KGS', 'CMS');

-- ----------------------------------------------------------------------------
-- 9. 科室默认患者ID类型 (loc_def_pat_id)
--    def_pat_id: PATID=患者ID, MRN=病历号(默认), ACCT=账户号
-- ----------------------------------------------------------------------------
INSERT INTO loc_def_pat_id (loc_num, def_pat_id)
VALUES ('a1000000-0000-0000-0000-000000000002', 'MRN');
INSERT INTO loc_def_pat_id (loc_num, def_pat_id)
VALUES ('a1000000-0000-0000-0000-000000000003', 'MRN');
INSERT INTO loc_def_pat_id (loc_num, def_pat_id)
VALUES ('a1000000-0000-0000-0000-000000000004', 'MRN');
INSERT INTO loc_def_pat_id (loc_num, def_pat_id)
VALUES ('a1000000-0000-0000-0000-000000000005', 'MRN');

-- ----------------------------------------------------------------------------
-- 10. 医生信息 (PHYSICIANS)
--     Physician_ID 为 PK (企业医生ID)
-- ----------------------------------------------------------------------------
INSERT INTO PHYSICIANS (Physician_ID, Last_Name, First_Name, Middle_Name, prefix, suffix, add_date)
VALUES ('PHY001', 'House', 'Gregory', 'J', 'Dr', 'MD', '2026-01-01 08:00:00');
INSERT INTO PHYSICIANS (Physician_ID, Last_Name, First_Name, Middle_Name, prefix, suffix, add_date)
VALUES ('PHY002', 'Cuddy', 'Lisa', 'C', 'Dr', 'MD', '2026-01-01 08:00:00');
INSERT INTO PHYSICIANS (Physician_ID, Last_Name, First_Name, Middle_Name, prefix, suffix, add_date)
VALUES ('PHY003', 'Wilson', 'James', 'E', 'Dr', 'MD', '2026-01-01 08:00:00');

-- ----------------------------------------------------------------------------
-- 11. 医生科室关联 (physician_to_unit)
--     PK (physician_id, loc_num); is_active: T=在职, F=离职
-- ----------------------------------------------------------------------------
INSERT INTO physician_to_unit (physician_id, loc_num, is_active)
VALUES ('PHY001', 'a1000000-0000-0000-0000-000000000002', 'T');
INSERT INTO physician_to_unit (physician_id, loc_num, is_active)
VALUES ('PHY002', 'a1000000-0000-0000-0000-000000000003', 'T');
INSERT INTO physician_to_unit (physician_id, loc_num, is_active)
VALUES ('PHY003', 'a1000000-0000-0000-0000-000000000004', 'T');
INSERT INTO physician_to_unit (physician_id, loc_num, is_active)
VALUES ('PHY001', 'a1000000-0000-0000-0000-000000000005', 'T');

-- ----------------------------------------------------------------------------
-- 12. 医护人员 (OPERATORS)
--     operator_num=内部UUID(PK), operator_id=登录ID, is_supervisor=T/F
--     last_update_date 有 UNIQUE 约束, 必须使用不同时间戳
--     arch: T=归档中, F=正常
-- ----------------------------------------------------------------------------
-- 管理员
INSERT INTO OPERATORS (operator_num, supervisor_num, operator_id, is_supervisor, arch, last_update_date, add_date)
VALUES ('b1000000-0000-0000-0000-000000000001', NULL, 'admin', 'T', 'F', '2026-01-01 08:00:01', '2026-01-01 08:00:00');
-- 技术员
INSERT INTO OPERATORS (operator_num, supervisor_num, operator_id, is_supervisor, arch, last_update_date, add_date)
VALUES ('b1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 'op002', 'F', 'F', '2026-01-01 08:00:02', '2026-01-01 08:00:00');
-- 普通操作员
INSERT INTO OPERATORS (operator_num, supervisor_num, operator_id, is_supervisor, arch, last_update_date, add_date)
VALUES ('b1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'op003', 'F', 'F', '2026-01-01 08:00:03', '2026-01-01 08:00:00');
-- 普通操作员
INSERT INTO OPERATORS (operator_num, supervisor_num, operator_id, is_supervisor, arch, last_update_date, add_date)
VALUES ('b1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000001', 'op004', 'F', 'F', '2026-01-01 08:00:04', '2026-01-01 08:00:00');

-- ----------------------------------------------------------------------------
-- 13. 医护人员权限 (operator_privilege)
--     PK (operator_num, inst_type, test_name)
--     privilege: 1=Supervisor, 4=User, 5=Service, 6=Training
--     is_active: T=活跃, F=停用
-- ----------------------------------------------------------------------------
-- 管理员: 全部检测项 supervisor 权限
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000001', 'StatStrip', 'Glu', 1, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000001', 'StatStrip', 'Ket', 1, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000001', 'StatStrip', 'Lac', 1, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000001', 'StatStrip', 'Hct', 1, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
-- 技术员: Glu/Ket User 权限
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000002', 'StatStrip', 'Glu', 4, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000002', 'StatStrip', 'Ket', 4, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
-- 普通操作员王强: Glu
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000003', 'StatStrip', 'Glu', 4, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
-- 普通操作员赵丽: Glu/Lac
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000004', 'StatStrip', 'Glu', 4, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');
INSERT INTO operator_privilege (operator_num, inst_type, test_name, privilege, is_active, cert_start_date, cert_end_date)
VALUES ('b1000000-0000-0000-0000-000000000004', 'StatStrip', 'Lac', 4, 'T', '2026-01-01 00:00:00', '2027-12-31 00:00:00');

-- ----------------------------------------------------------------------------
-- 14. 医护人员科室关联 (operator_to_unit)
--     PK (operator_num, loc_num); is_default: T=默认科室
-- ----------------------------------------------------------------------------
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', 'T', 'T');
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003', 'F', 'T');
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002', 'T', 'T');
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000003', 'T', 'T');
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000004', 'T', 'T');
INSERT INTO operator_to_unit (operator_num, loc_num, is_default, is_active)
VALUES ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000005', 'F', 'T');

-- ----------------------------------------------------------------------------
-- 15. 仪器检测项目定义 (INSTRUMENTS_TESTS)
--     PK instruments_tests_num(UUID); inst_type FK INSTRUMENT_TYPES
--     test_code: LOINC 编码; result_type_code: M=Measured, C=Calc, I=Input
--     send_to_inst: T=可发送到仪器
-- ----------------------------------------------------------------------------
INSERT INTO INSTRUMENTS_TESTS (inst_type, test_code, sample_type_code, result_type_code, units, lo_limit, hi_limit, resolution, test_name, generic_test_name, valid_sar_test, instruments_tests_num, test_transmit_name, send_to_inst, test_code_system)
VALUES ('StatStrip', '2341-6', '11779-6', 'M', 'mg/dL', '20', '600', '1', 'Glucose', 'Glu', 'F', 'c1000000-0000-0000-0000-000000000001', 'Glu', 'T', 'LOINC');
INSERT INTO INSTRUMENTS_TESTS (inst_type, test_code, sample_type_code, result_type_code, units, lo_limit, hi_limit, resolution, test_name, generic_test_name, valid_sar_test, instruments_tests_num, test_transmit_name, send_to_inst, test_code_system)
VALUES ('StatStrip', '3394-6', '11779-6', 'M', 'mmol/L', '0', '8', '0.1', 'Ketone', 'Ket', 'F', 'c1000000-0000-0000-0000-000000000002', 'Ket', 'T', 'LOINC');
INSERT INTO INSTRUMENTS_TESTS (inst_type, test_code, sample_type_code, result_type_code, units, lo_limit, hi_limit, resolution, test_name, generic_test_name, valid_sar_test, instruments_tests_num, test_transmit_name, send_to_inst, test_code_system)
VALUES ('StatStrip', '3261-5', '11779-6', 'M', 'mmol/L', '0', '20', '0.1', 'Lactate', 'Lac', 'F', 'c1000000-0000-0000-0000-000000000003', 'Lac', 'T', 'LOINC');
INSERT INTO INSTRUMENTS_TESTS (inst_type, test_code, sample_type_code, result_type_code, units, lo_limit, hi_limit, resolution, test_name, generic_test_name, valid_sar_test, instruments_tests_num, test_transmit_name, send_to_inst, test_code_system)
VALUES ('StatStrip', '20570-8', '11779-6', 'M', '%', '10', '80', '1', 'Hematocrit', 'Hct', 'F', 'c1000000-0000-0000-0000-000000000004', 'Hct', 'T', 'LOINC');

-- ----------------------------------------------------------------------------
-- 16. 仪器设备实例 (instruments)
--     inst_num=UUID(PK), inst_id=设备序列号(UNIQUE), loc_num=所属科室
--     inst_condition: R=Ready, B=Busy, L=QC Lockout, S=Standby
-- ----------------------------------------------------------------------------
INSERT INTO instruments (inst_num, inst_type, inst_name, inst_id, serial_no, loc_num, inst_active, sw_version, inst_condition, computer_name, ip_address)
VALUES ('d1000000-0000-0000-0000-000000000001', 'StatStrip', 'StatStrip-ICU-01', 'SS-001', 'SN001', 'a1000000-0000-0000-0000-000000000002', 1, '3.2.1', 'R', 'NOVA-SERVER', '192.168.1.101');
INSERT INTO instruments (inst_num, inst_type, inst_name, inst_id, serial_no, loc_num, inst_active, sw_version, inst_condition, computer_name, ip_address)
VALUES ('d1000000-0000-0000-0000-000000000002', 'StatStrip', 'StatStrip-ER-01', 'SS-002', 'SN002', 'a1000000-0000-0000-0000-000000000003', 1, '3.2.1', 'R', 'NOVA-SERVER', '192.168.1.102');
INSERT INTO instruments (inst_num, inst_type, inst_name, inst_id, serial_no, loc_num, inst_active, sw_version, inst_condition, computer_name, ip_address)
VALUES ('d1000000-0000-0000-0000-000000000003', 'StatStrip', 'StatStrip-GS-01', 'SS-003', 'SN003', 'a1000000-0000-0000-0000-000000000004', 1, '3.2.1', 'R', 'NOVA-SERVER', '192.168.1.103');

-- ----------------------------------------------------------------------------
-- 17. 通信配置 (Communications)
--     PK comm_record_num(UUID); UNIQUE (Computer_Name, Instrument_ID, Port_Num)
--     Comm_Protocol: TCPIP/Serial; Port_Type: Analyzer/LIS/ADT
--     Connect_Remote: 0=监听, 1=主动连接
-- ----------------------------------------------------------------------------
INSERT INTO Communications (Computer_Name, Instrument_ID, Protocol, Port_Type, Comm_Protocol, Port_Num, Connect_Remote, Port_Active, Remote_Port, Rcv_Application, Rcv_Facility, InstrumentUUID, Used, Multi_Connect, comm_record_num, from_ui)
VALUES ('NOVA-SERVER', 'SS-001', 'DML', 'Analyzer', 'TCPIP', 57381, 0, 1, 0, 'BIO-CONNECT', 'NOVA', 'd1000000-0000-0000-0000-000000000001', 'T', 'F', 'e1000000-0000-0000-0000-000000000001', 'T');
INSERT INTO Communications (Computer_Name, Instrument_ID, Protocol, Port_Type, Comm_Protocol, Port_Num, Connect_Remote, Port_Active, Remote_Port, Rcv_Application, Rcv_Facility, InstrumentUUID, Used, Multi_Connect, comm_record_num, from_ui)
VALUES ('NOVA-SERVER', 'SS-002', 'DML', 'Analyzer', 'TCPIP', 57381, 0, 1, 0, 'BIO-CONNECT', 'NOVA', 'd1000000-0000-0000-0000-000000000002', 'T', 'F', 'e1000000-0000-0000-0000-000000000002', 'T');
INSERT INTO Communications (Computer_Name, Instrument_ID, Protocol, Port_Type, Comm_Protocol, Port_Num, Connect_Remote, Port_Active, Remote_Port, Rcv_Application, Rcv_Facility, InstrumentUUID, Used, Multi_Connect, comm_record_num, from_ui)
VALUES ('NOVA-SERVER', 'SS-003', 'DML', 'Analyzer', 'TCPIP', 57381, 0, 1, 0, 'BIO-CONNECT', 'NOVA', 'd1000000-0000-0000-0000-000000000003', 'T', 'F', 'e1000000-0000-0000-0000-000000000003', 'T');

-- ----------------------------------------------------------------------------
-- 18. 试剂/质控批号 (LOTS)
--     lots_key_num=UUID(PK); lot_type: TestStrip/Control/Linearity
--     in_use: T=使用中; retired: T=已退役; is_validated: T=已验证
-- ----------------------------------------------------------------------------
-- 试纸批号
INSERT INTO LOTS (lots_key_num, lot, expDate, lot_type, lot_name, in_use, retired, is_validated, usedCount, Remaining, use_before)
VALUES ('f1000000-0000-0000-0000-000000000001', 'TS2026A', '2027-12-31', 'TestStrip', 'StatStrip 试纸批号 2026A', 'T', 'F', 'T', 0, '100', '2027-12-31');
-- 质控批号 Level 1 (正常水平)
INSERT INTO LOTS (lots_key_num, lot, expDate, lot_type, lot_name, in_use, retired, is_validated, usedCount, Remaining, use_before)
VALUES ('f1000000-0000-0000-0000-000000000002', 'QC2026L1', '2027-06-30', 'Control', '质控液 Level 1 (正常)', 'T', 'F', 'T', 0, '50', '2027-06-30');
-- 质控批号 Level 2 (异常高水平)
INSERT INTO LOTS (lots_key_num, lot, expDate, lot_type, lot_name, in_use, retired, is_validated, usedCount, Remaining, use_before)
VALUES ('f1000000-0000-0000-0000-000000000003', 'QC2026L2', '2027-06-30', 'Control', '质控液 Level 2 (异常高)', 'T', 'F', 'T', 0, '50', '2027-06-30');

-- ----------------------------------------------------------------------------
-- 19. 批号支持的设备类型 (device_to_lot)
--     PK (lots_key_num, inst_type)
-- ----------------------------------------------------------------------------
INSERT INTO device_to_lot (lots_key_num, inst_type)
VALUES ('f1000000-0000-0000-0000-000000000001', 'StatStrip');
INSERT INTO device_to_lot (lots_key_num, inst_type)
VALUES ('f1000000-0000-0000-0000-000000000002', 'StatStrip');
INSERT INTO device_to_lot (lots_key_num, inst_type)
VALUES ('f1000000-0000-0000-0000-000000000003', 'StatStrip');

-- ----------------------------------------------------------------------------
-- 20. 批号分配到科室 (lots_to_unit)
--     PK (lots_key_num, loc_num); loc_num FK inst_locations
-- ----------------------------------------------------------------------------
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001');
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001');
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000001');
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001');
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001');
INSERT INTO lots_to_unit (lots_key_num, loc_num, fac_num)
VALUES ('f1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001');

-- ----------------------------------------------------------------------------
-- 21. 批号化学范围 (LOT_CHEM)
--     PK (lots_key_num, generic_test_name, Units, facility_num)
--     facility_num FK inst_locations; LR/HR=范围上下限; TM/TSD=靶值均值/标准差
-- ----------------------------------------------------------------------------
-- Level 1 质控液: 各项目正常范围
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000002', 'Glu', '1', 'Control', '70', '130', 100.0, 5.0, 'mg/dL', 'a1000000-0000-0000-0000-000000000001', 1, 0, 'F', 2);
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000002', 'Ket', '1', 'Control', '0', '6', 3.0, 0.5, 'mmol/L', 'a1000000-0000-0000-0000-000000000001', 2, 1, 'F', 2);
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000002', 'Lac', '1', 'Control', '0', '10', 5.0, 0.8, 'mmol/L', 'a1000000-0000-0000-0000-000000000001', 3, 1, 'F', 2);
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000002', 'Hct', '1', 'Control', '20', '60', 40.0, 2.0, '%', 'a1000000-0000-0000-0000-000000000001', 4, 0, 'F', 2);
-- Level 2 质控液: 异常高值范围
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000003', 'Glu', '2', 'Control', '250', '350', 300.0, 10.0, 'mg/dL', 'a1000000-0000-0000-0000-000000000001', 1, 0, 'F', 2);
INSERT INTO LOT_CHEM (lots_key_num, generic_test_name, lot_level, level_type, LR, HR, TM, TSD, Units, facility_num, display_order, dps, order_calc_flag, range_method)
VALUES ('f1000000-0000-0000-0000-000000000003', 'Ket', '2', 'Control', '3', '8', 5.5, 0.6, 'mmol/L', 'a1000000-0000-0000-0000-000000000001', 2, 1, 'F', 2);

-- ----------------------------------------------------------------------------
-- 22. 患者基本信息 (PATIENTS)
--     patient_uuid=UUID(PK); Patient_ID=企业ID; medrec_num=病历号(NOT NULL)
--     facil_num=机构UUID(NOT NULL, FK inst_locations)
--     last_activity_Date 有 UNIQUE 约束, 必须使用不同时间戳
--     arch: T=归档中, F=正常
-- ----------------------------------------------------------------------------
-- 张三 (ICU在院)
INSERT INTO PATIENTS (Patient_ID, Last_Name, First_Name, Sex, birthdate, last_activity_Date, arch, patient_uuid, medrec_num, add_date, last_update_date, facil_num, race)
VALUES ('PID001', '张', '三', 'M', '1980-05-15', '2026-01-15 09:30:01', 'F', 'g1000000-0000-0000-0000-000000000001', 'MRN001', '2026-01-15 09:00:00', '2026-01-15 09:30:00', 'a1000000-0000-0000-0000-000000000001', 'Asian');
-- 李四 (急诊在院)
INSERT INTO PATIENTS (Patient_ID, Last_Name, First_Name, Sex, birthdate, last_activity_Date, arch, patient_uuid, medrec_num, add_date, last_update_date, facil_num, race)
VALUES ('PID002', '李', '四', 'F', '1975-08-20', '2026-01-20 14:15:02', 'F', 'g1000000-0000-0000-0000-000000000002', 'MRN002', '2026-01-20 14:00:00', '2026-01-20 14:15:00', 'a1000000-0000-0000-0000-000000000001', 'Asian');
-- 王五 (普外科在院)
INSERT INTO PATIENTS (Patient_ID, Last_Name, First_Name, Sex, birthdate, last_activity_Date, arch, patient_uuid, medrec_num, add_date, last_update_date, facil_num, race)
VALUES ('PID003', '王', '五', 'M', '1990-03-10', '2026-01-25 10:00:03', 'F', 'g1000000-0000-0000-0000-000000000003', 'MRN003', '2026-01-25 09:30:00', '2026-01-25 10:00:00', 'a1000000-0000-0000-0000-000000000001', 'Asian');
-- 孙六 (内科已出院)
INSERT INTO PATIENTS (Patient_ID, Last_Name, First_Name, Sex, birthdate, last_activity_Date, arch, patient_uuid, medrec_num, add_date, last_update_date, facil_num, race)
VALUES ('PID004', '孙', '六', 'M', '1968-11-25', '2026-01-28 16:00:04', 'T', 'g1000000-0000-0000-0000-000000000004', 'MRN004', '2026-01-10 08:00:00', '2026-01-28 16:00:00', 'a1000000-0000-0000-0000-000000000001', 'Asian');

-- ----------------------------------------------------------------------------
-- 23. 患者账户 (PATIENT_ACCOUNTS)
--     account_uuid=UUID(PK); patient_uuid FK PATIENTS
--     last_activity_Date 有 UNIQUE 约束
-- ----------------------------------------------------------------------------
INSERT INTO PATIENT_ACCOUNTS (last_activity_Date, arch, patient_uuid, account_num, add_date)
VALUES ('2026-01-15 09:30:11', 'F', 'g1000000-0000-0000-0000-000000000001', 'ACC001', '2026-01-15 09:00:00');
INSERT INTO PATIENT_ACCOUNTS (last_activity_Date, arch, patient_uuid, account_num, add_date)
VALUES ('2026-01-20 14:15:12', 'F', 'g1000000-0000-0000-0000-000000000002', 'ACC002', '2026-01-20 14:00:00');
INSERT INTO PATIENT_ACCOUNTS (last_activity_Date, arch, patient_uuid, account_num, add_date)
VALUES ('2026-01-25 10:00:13', 'F', 'g1000000-0000-0000-0000-000000000003', 'ACC003', '2026-01-25 09:30:00');
INSERT INTO PATIENT_ACCOUNTS (last_activity_Date, arch, patient_uuid, account_num, add_date)
VALUES ('2026-01-28 16:00:14', 'T', 'g1000000-0000-0000-0000-000000000004', 'ACC004', '2026-01-10 08:00:00');

-- ----------------------------------------------------------------------------
-- 24. 患者就诊记录 (PATIENT_VISITS)
--     visit_uuid=UUID(PK); patient_uuid FK PATIENTS; account_uuid FK PATIENT_ACCOUNTS
--     loc_num NOT NULL FK inst_locations; last_activity_Date 有 UNIQUE 约束
--     patient_class: I=住院, E=急诊, O=门诊
-- ----------------------------------------------------------------------------
-- 张三 ICU 住院
INSERT INTO PATIENT_VISITS (last_activity_Date, arch, patient_uuid, account_uuid, Attend_Physician, Report_Physician, visit_num, admit_time, patient_class, patient_type, loc_num, room_num, bed_num, add_date)
VALUES ('2026-01-15 09:30:21', 'F', 'g1000000-0000-0000-0000-000000000001', 'g1000000-0000-0000-0000-000000000001', 'PHY001', 'PHY001', 'V2026001', '2026-01-15 09:30:00', 'I', 'Inpatient', 'a1000000-0000-0000-0000-000000000002', '101', 'A', '2026-01-15 09:00:00');
-- 李四 急诊
INSERT INTO PATIENT_VISITS (last_activity_Date, arch, patient_uuid, account_uuid, Attend_Physician, Report_Physician, visit_num, admit_time, patient_class, patient_type, loc_num, room_num, bed_num, add_date)
VALUES ('2026-01-20 14:15:22', 'F', 'g1000000-0000-0000-0000-000000000002', 'g1000000-0000-0000-0000-000000000002', 'PHY002', 'PHY002', 'V2026002', '2026-01-20 14:15:00', 'E', 'Emergency', 'a1000000-0000-0000-0000-000000000003', '201', 'B', '2026-01-20 14:00:00');
-- 王五 普外科住院
INSERT INTO PATIENT_VISITS (last_activity_Date, arch, patient_uuid, account_uuid, Attend_Physician, Report_Physician, visit_num, admit_time, patient_class, patient_type, loc_num, room_num, bed_num, add_date)
VALUES ('2026-01-25 10:00:23', 'F', 'g1000000-0000-0000-0000-000000000003', 'g1000000-0000-0000-0000-000000000003', 'PHY003', 'PHY003', 'V2026003', '2026-01-25 10:00:00', 'I', 'Inpatient', 'a1000000-0000-0000-0000-000000000004', '301', 'C', '2026-01-25 09:30:00');
-- 孙六 内科 (已出院)
INSERT INTO PATIENT_VISITS (last_activity_Date, arch, patient_uuid, account_uuid, Attend_Physician, Report_Physician, visit_num, admit_time, discharge_time, patient_class, patient_type, loc_num, room_num, bed_num, add_date)
VALUES ('2026-01-28 16:00:24', 'T', 'g1000000-0000-0000-0000-000000000004', 'g1000000-0000-0000-0000-000000000004', 'PHY001', 'PHY001', 'V2026004', '2026-01-10 08:00:00', '2026-01-28 16:00:00', 'I', 'Inpatient', 'a1000000-0000-0000-0000-000000000005', '401', 'D', '2026-01-10 08:00:00');

-- ----------------------------------------------------------------------------
-- 25. 服务健康状态 (health_ping)
--     PK (process_name, host); 记录 RTM 服务心跳
-- ----------------------------------------------------------------------------
INSERT INTO health_ping (process_name, host, update_time, do_log, last_start_dttm, last_connect_dttm, tot_messages_processed)
VALUES ('RTMADTP', 'NOVA-SERVER', '2026-01-01 00:00:00', 'T', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);
INSERT INTO health_ping (process_name, host, update_time, do_log, last_start_dttm, last_connect_dttm, tot_messages_processed)
VALUES ('RTMOPL', 'NOVA-SERVER', '2026-01-01 00:00:00', 'T', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);
INSERT INTO health_ping (process_name, host, update_time, do_log, last_start_dttm, last_connect_dttm, tot_messages_processed)
VALUES ('RTMLIS', 'NOVA-SERVER', '2026-01-01 00:00:00', 'T', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);

-- ============================================================================
-- 初始化数据完成
-- 数据统计:
--   time_zone: 3, INSTRUMENT_TYPES: 1, inst_locations: 5, SAMPLE_TYPES: 3
--   config_data: 10, loc_to_config: 11, facility_prefs: 1, facility_ht_wt_units: 1
--   loc_def_pat_id: 4, PHYSICIANS: 3, physician_to_unit: 4
--   OPERATORS: 4, operator_privilege: 9, operator_to_unit: 6
--   INSTRUMENTS_TESTS: 4, instruments: 3, Communications: 3
--   LOTS: 3, device_to_lot: 3, lots_to_unit: 6, LOT_CHEM: 6
--   PATIENTS: 4, PATIENT_ACCOUNTS: 4, PATIENT_VISITS: 4
--   health_ping: 3
-- ============================================================================
COMMIT;
