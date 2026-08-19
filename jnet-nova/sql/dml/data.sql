-- ============================================================================
-- NovaNet 初始化数据 - 基于 novanet.sql 表结构梳理的完整业务关联数据
-- ============================================================================
-- 业务领域: POCT (Point of Care Testing) 床旁检测数据管理系统
-- 业务主线:
--   1. 组织机构 (inst_locations): 机构 → 科室 层级
--   2. 医护人员 (operators): 基本信息 + 权限 + 科室关联 + 检测项目
--   3. 患者 (patients): 基本信息 + 账户 + 就诊(科室/床位/医生)
--   4. 医生 (physicians): 主治医生 + 科室关联
--   5. 仪器 (instruments): 设备类型 + 检测项目 + 通信配置
--   6. 试剂批号 (lots): 试纸/质控批号 + 设备/科室关联 + 化学范围
--   7. 配置 (config_data): 系统参数 + 科室级配置
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 组织机构层级 (对应 novanet.inst_locations)
--    层级: level 1 = 机构(Facility), level 2 = 科室(Unit/Department)
-- ----------------------------------------------------------------------------

-- 总医院 (根机构)
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, description, is_default, inst_class, inst_type, status) KEY(loc_num)
VALUES ('LOC-FAC-001', '中心医院', NULL, 1, '中心医院', '总院机构', 'T', 'POCT', 'StatStrip', 'Active');

-- ICU 重症监护室
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, description, is_default, inst_class, inst_type, status) KEY(loc_num)
VALUES ('LOC-ICU-001', 'ICU重症监护室', 'LOC-FAC-001', 2, '中心医院', '重症监护科室', 'T', 'POCT', 'StatStrip', 'Active');

-- 急诊科
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, description, is_default, inst_class, inst_type, status) KEY(loc_num)
VALUES ('LOC-ER-001', '急诊科', 'LOC-FAC-001', 2, '中心医院', '急诊抢救科室', 'F', 'POCT', 'StatStrip', 'Active');

-- 普外科
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, description, is_default, inst_class, inst_type, status) KEY(loc_num)
VALUES ('LOC-GS-001', '普外科', 'LOC-FAC-001', 2, '中心医院', '普通外科病房', 'F', 'POCT', 'StatStrip', 'Active');

-- 内科
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, description, is_default, inst_class, inst_type, status) KEY(loc_num)
VALUES ('LOC-IM-001', '内科', 'LOC-FAC-001', 2, '中心医院', '内科病房', 'F', 'POCT', 'StatStrip', 'Active');

-- ----------------------------------------------------------------------------
-- 2. 科室数据最后更新时间 (对应 novanet.loc_last_update)
--    用于增量同步追踪: OPERATORS/PATIENTS/PHYSICIANS/LOTS/SETUP 等
-- ----------------------------------------------------------------------------
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'OPERATORS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'PATIENTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'PHYSICIANS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'LOTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'SETUP', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ICU-001', 'LOCATIONS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ER-001', 'OPERATORS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ER-001', 'PATIENTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ER-001', 'PHYSICIANS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ER-001', 'LOTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-ER-001', 'SETUP', '2026-01-01 00:00:00', 'POCT', 'StatStrip');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-GS-001', 'OPERATORS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-GS-001', 'PATIENTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-GS-001', 'PHYSICIANS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-GS-001', 'LOTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-IM-001', 'OPERATORS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-IM-001', 'PATIENTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-IM-001', 'PHYSICIANS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time, inst_class, inst_type) KEY(loc_num, data_type, inst_class, inst_type)
VALUES ('LOC-IM-001', 'LOTS', '2026-01-01 00:00:00', 'POCT', 'StatStrip');

-- ----------------------------------------------------------------------------
-- 3. 系统配置数据 (对应 novanet.config_data)
--    Key-Value 形式存储各类 ID 长度/格式限制等
-- ----------------------------------------------------------------------------
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG001', 'patient_id_max_length*V', '20');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG002', 'patient_id_min_length*V', '1');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG003', 'operator_id_max_length*V', '20');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG004', 'operator_id_min_length*V', '1');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG005', 'accn_id_max_length*V', '16');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG006', 'accn_id_min_length*V', '1');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG007', 'allow_testing_default_unit*V', '1');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG008', 'obs_review_no_login*V', '1');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG009', 'mrn_max_length*V', '20');
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG010', 'mrn_min_length*V', '1');

-- ----------------------------------------------------------------------------
-- 4. 科室配置关联 (对应 novanet.loc_to_config)
--    将配置项绑定到具体科室
-- ----------------------------------------------------------------------------
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG001', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG002', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG003', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG004', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG005', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG007', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ICU-001', 'CFG008', 'StatStrip', 'POCT');

MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ER-001', 'CFG001', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ER-001', 'CFG003', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ER-001', 'CFG005', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-ER-001', 'CFG007', 'StatStrip', 'POCT');

MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-GS-001', 'CFG001', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-GS-001', 'CFG003', 'StatStrip', 'POCT');

MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-IM-001', 'CFG001', 'StatStrip', 'POCT');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type, inst_class) KEY(loc_num, config_num)
VALUES ('LOC-IM-001', 'CFG003', 'StatStrip', 'POCT');

-- ----------------------------------------------------------------------------
-- 5. 仪器检测项目定义 (对应 novanet.instruments_tests)
--    StatStrip 设备支持的检测项: 血糖/酮体/乳酸/红细胞压积
-- ----------------------------------------------------------------------------
MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Glu', 'Glu', '2341-6', 'NM', 'mg/dL', '80', '120', 1);
MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Ket', 'Ket', '3394-6', 'NM', 'mmol/L', '0', '6', 2);
MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Lac', 'Lac', '3261-5', 'NM', 'mmol/L', '0', '10', 3);
MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Hct', 'Hct', '20570-8', 'NM', '%', '20', '60', 4);

-- 检测项校准偏移 (对应 novanet.test_offsets)
MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Glu', 'StatStrip', 'mg/dL', '1', '0');
MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Ket', 'StatStrip', 'mmol/L', '1', '0');
MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Lac', 'StatStrip', 'mmol/L', '1', '0');
MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Hct', 'StatStrip', '%', '1', '0');

-- ----------------------------------------------------------------------------
-- 6. 试剂/质控批号 (对应 novanet.lots)
--    lot_type: TestStrip=试纸, Control=质控, Linearity=线性
-- ----------------------------------------------------------------------------
-- 试纸批号
MERGE INTO dml_lot (lots_key_num, lot, lot_type, lot_name, exp_date, in_use, used_count, retired, is_validated) KEY(lots_key_num)
VALUES ('LOT-KEY-001', 'TS2026A', 'TestStrip', 'StatStrip 试纸批号 2026A', '2027-12-31', 'T', 0, 'F', 'T');
-- 质控批号 Level 1 (正常水平)
MERGE INTO dml_lot (lots_key_num, lot, lot_type, lot_name, exp_date, in_use, used_count, retired, is_validated) KEY(lots_key_num)
VALUES ('LOT-KEY-002', 'QC2026L1', 'Control', '质控液 Level 1 (正常)', '2027-06-30', 'T', 0, 'F', 'T');
-- 质控批号 Level 2 (异常高水平)
MERGE INTO dml_lot (lots_key_num, lot, lot_type, lot_name, exp_date, in_use, used_count, retired, is_validated) KEY(lots_key_num)
VALUES ('LOT-KEY-003', 'QC2026L2', 'Control', '质控液 Level 2 (异常高)', '2027-06-30', 'T', 0, 'F', 'T');

-- 批号支持的设备类型 (对应 novanet.device_to_lot)
MERGE INTO dml_device_to_lot (lots_key_num, inst_type) KEY(lots_key_num, inst_type)
VALUES ('LOT-KEY-001', 'StatStrip');
MERGE INTO dml_device_to_lot (lots_key_num, inst_type) KEY(lots_key_num, inst_type)
VALUES ('LOT-KEY-002', 'StatStrip');
MERGE INTO dml_device_to_lot (lots_key_num, inst_type) KEY(lots_key_num, inst_type)
VALUES ('LOT-KEY-003', 'StatStrip');

-- 批号分配到科室 (对应 novanet.lots_to_unit)
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-001', 'LOC-ICU-001');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-001', 'LOC-ER-001');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-001', 'LOC-GS-001');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-002', 'LOC-ICU-001');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-002', 'LOC-ER-001');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT-KEY-003', 'LOC-ICU-001');

-- 批号化学范围 (对应 novanet.lot_chem)
-- Level 1 质控液各项目靶值范围
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-002', 'Glu', 'Glu', '2341-6', '70', '130', 'mg/dL', '1', 'Control');
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-002', 'Ket', 'Ket', '3394-6', '0', '6', 'mmol/L', '1', 'Control');
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-002', 'Lac', 'Lac', '3261-5', '0', '10', 'mmol/L', '1', 'Control');
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-002', 'Hct', 'Hct', '20570-8', '20', '60', '%', '1', 'Control');
-- Level 2 质控液 (异常高值)
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-003', 'Glu', 'Glu', '2341-6', '250', '350', 'mg/dL', '2', 'Control');
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT-KEY-003', 'Ket', 'Ket', '3394-6', '3', '8', 'mmol/L', '2', 'Control');

-- ----------------------------------------------------------------------------
-- 7. 医生信息 (对应 novanet.physicians + physician_to_unit)
-- ----------------------------------------------------------------------------
MERGE INTO dml_physician (physician_id, physician_name, first_name, last_name, prefix, suffix, facility, location, loc_num, department, status) KEY(physician_id)
VALUES ('PHY001', 'Gregory House', 'Gregory', 'House', 'Dr', 'MD', '中心医院', 'ICU重症监护室', 'LOC-ICU-001', '重症医学科', 'Active');
MERGE INTO dml_physician (physician_id, physician_name, first_name, last_name, prefix, suffix, facility, location, loc_num, department, status) KEY(physician_id)
VALUES ('PHY002', 'Lisa Cuddy', 'Lisa', 'Cuddy', 'Dr', 'MD', '中心医院', '急诊科', 'LOC-ER-001', '急诊科', 'Active');
MERGE INTO dml_physician (physician_id, physician_name, first_name, last_name, prefix, suffix, facility, location, loc_num, department, status) KEY(physician_id)
VALUES ('PHY003', 'James Wilson', 'James', 'Wilson', 'Dr', 'MD', '中心医院', '普外科', 'LOC-GS-001', '普外科', 'Active');

-- ----------------------------------------------------------------------------
-- 8. 医护人员 (对应 novanet.operators)
--    privilege_level: 1=Supervisor管理员, 2=Technician技术员, 4=User普通用户
--    is_supervisor: T=主管, F=非主管
-- ----------------------------------------------------------------------------
-- 管理员 (覆盖全院科室)
MERGE INTO dml_operator (operator_num, operator_id, operator_name, first_name, last_name, email, is_supervisor, privilege_level, facility, location, loc_num, department, status) KEY(operator_num)
VALUES ('OP-NUM-001', 'admin', '张明', '明', '张', 'zhangming@hospital.cn', 'T', 1, '中心医院', 'ICU重症监护室', 'LOC-ICU-001', '信息科', 'A');
-- 技术员 (ICU)
MERGE INTO dml_operator (operator_num, operator_id, operator_name, first_name, last_name, email, is_supervisor, privilege_level, facility, location, loc_num, department, status) KEY(operator_num)
VALUES ('OP-NUM-002', 'op002', '李芳', '芳', '李', 'lifang@hospital.cn', 'F', 2, '中心医院', 'ICU重症监护室', 'LOC-ICU-001', '检验科', 'A');
-- 普通操作员 (急诊)
MERGE INTO dml_operator (operator_num, operator_id, operator_name, first_name, last_name, email, is_supervisor, privilege_level, facility, location, loc_num, department, status) KEY(operator_num)
VALUES ('OP-NUM-003', 'op003', '王强', '强', '王', 'wangqiang@hospital.cn', 'F', 4, '中心医院', '急诊科', 'LOC-ER-001', '急诊科', 'A');
-- 普通操作员 (普外科 + 内科)
MERGE INTO dml_operator (operator_num, operator_id, operator_name, first_name, last_name, email, is_supervisor, privilege_level, facility, location, loc_num, department, status) KEY(operator_num)
VALUES ('OP-NUM-004', 'op004', '赵丽', '丽', '赵', 'zhaoli@hospital.cn', 'F', 4, '中心医院', '普外科', 'LOC-GS-001', '普外科', 'A');

-- ----------------------------------------------------------------------------
-- 9. 医护人员权限 (对应 novanet.operator_privilege)
--    privilege_code: SUPERVISOR=管理员, TECH=技术员, USER=普通用户
-- ----------------------------------------------------------------------------
MERGE INTO dml_operator_privilege (operator_num, inst_type, privilege_code, privilege_desc) KEY(operator_num, inst_type, privilege_code)
VALUES ('OP-NUM-001', 'StatStrip', 'SUPERVISOR', '管理员权限-全部操作');
MERGE INTO dml_operator_privilege (operator_num, inst_type, privilege_code, privilege_desc) KEY(operator_num, inst_type, privilege_code)
VALUES ('OP-NUM-002', 'StatStrip', 'TECH', '技术员权限-检测与质控');
MERGE INTO dml_operator_privilege (operator_num, inst_type, privilege_code, privilege_desc) KEY(operator_num, inst_type, privilege_code)
VALUES ('OP-NUM-003', 'StatStrip', 'USER', '普通用户权限-仅检测');
MERGE INTO dml_operator_privilege (operator_num, inst_type, privilege_code, privilege_desc) KEY(operator_num, inst_type, privilege_code)
VALUES ('OP-NUM-004', 'StatStrip', 'USER', '普通用户权限-仅检测');

-- ----------------------------------------------------------------------------
-- 10. 医护人员科室关联 (对应 novanet.operator_to_unit)
--     一个医护人员可关联多个科室
-- ----------------------------------------------------------------------------
-- 管理员覆盖 ICU + 急诊
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-001', 'LOC-ICU-001', 'ICU重症监护室');
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-001', 'LOC-ER-001', '急诊科');
-- 技术员仅 ICU
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-002', 'LOC-ICU-001', 'ICU重症监护室');
-- 王强在急诊
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-003', 'LOC-ER-001', '急诊科');
-- 赵丽在普外科 + 内科
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-004', 'LOC-GS-001', '普外科');
MERGE INTO dml_operator_to_unit (operator_num, loc_num, unit_name) KEY(operator_num, loc_num)
VALUES ('OP-NUM-004', 'LOC-IM-001', '内科');

-- ----------------------------------------------------------------------------
-- 11. 医护人员授权检测项目 (对应 novanet.methods)
--     操作员被授权可执行的检测方法
-- ----------------------------------------------------------------------------
-- 管理员可执行全部检测
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-001', 'StatStrip', '血糖检测', 'Glu');
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-001', 'StatStrip', '酮体检测', 'Ket');
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-001', 'StatStrip', '乳酸检测', 'Lac');
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-001', 'StatStrip', '红细胞压积', 'Hct');
-- 技术员可执行血糖+酮体
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-002', 'StatStrip', '血糖检测', 'Glu');
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-002', 'StatStrip', '酮体检测', 'Ket');
-- 王强可执行血糖
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-003', 'StatStrip', '血糖检测', 'Glu');
-- 赵丽可执行血糖+乳酸
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-004', 'StatStrip', '血糖检测', 'Glu');
MERGE INTO dml_method (operator_num, inst_type, method_name, method_code) KEY(operator_num, inst_type, method_code)
VALUES ('OP-NUM-004', 'StatStrip', '乳酸检测', 'Lac');

-- ----------------------------------------------------------------------------
-- 12. 患者基本信息 (对应 novanet.patients)
--     patient_num=内部UUID, patient_id=外部企业ID, medrec_num=病历号MRN
--     status: A=在院活跃, I=已出院/不活跃
-- ----------------------------------------------------------------------------
-- 患者张三 (ICU在院)
MERGE INTO dml_patient (patient_num, patient_id, medrec_num, account_num, first_name, last_name, birth_date, sex, race, address, phone_home, facility, location, loc_num, bed, room, physician_id, physician_name, status) KEY(patient_num)
VALUES ('PAT-NUM-001', 'PID001', 'MRN001', 'ACC001', '三', '张', '1980-05-15', 'M', 'Asian', '北京市朝阳区建国路100号', '010-88880001', '中心医院', 'ICU重症监护室', 'LOC-ICU-001', 'A', '101', 'PHY001', 'Gregory House', 'A');
-- 患者李四 (急诊在院)
MERGE INTO dml_patient (patient_num, patient_id, medrec_num, account_num, first_name, last_name, birth_date, sex, race, address, phone_home, facility, location, loc_num, bed, room, physician_id, physician_name, status) KEY(patient_num)
VALUES ('PAT-NUM-002', 'PID002', 'MRN002', 'ACC002', '四', '李', '1975-08-20', 'F', 'Asian', '北京市海淀区中关村路50号', '010-88880002', '中心医院', '急诊科', 'LOC-ER-001', 'B', '201', 'PHY002', 'Lisa Cuddy', 'A');
-- 患者王五 (普外科在院)
MERGE INTO dml_patient (patient_num, patient_id, medrec_num, account_num, first_name, last_name, birth_date, sex, race, address, phone_home, facility, location, loc_num, bed, room, physician_id, physician_name, status) KEY(patient_num)
VALUES ('PAT-NUM-003', 'PID003', 'MRN003', 'ACC003', '五', '王', '1990-03-10', 'M', 'Asian', '北京市西城区西直门大街20号', '010-88880003', '中心医院', '普外科', 'LOC-GS-001', 'C', '301', 'PHY003', 'James Wilson', 'A');
-- 患者孙六 (内科, 已出院)
MERGE INTO dml_patient (patient_num, patient_id, medrec_num, account_num, first_name, last_name, birth_date, sex, race, address, phone_home, facility, location, loc_num, bed, room, physician_id, physician_name, status) KEY(patient_num)
VALUES ('PAT-NUM-004', 'PID004', 'MRN004', 'ACC004', '六', '孙', '1968-11-25', 'M', 'Asian', '北京市东城区东直门内大街8号', '010-88880004', '中心医院', '内科', 'LOC-IM-001', 'D', '401', 'PHY001', 'Gregory House', 'I');

-- ----------------------------------------------------------------------------
-- 13. 患者账户 (对应 novanet.patient_accounts)
--     一个患者可对应多个账户(不同就诊机构)
-- ----------------------------------------------------------------------------
MERGE INTO dml_patient_account (account_num, patient_num, account_number, account_name, status) KEY(account_num)
VALUES ('ACCT-NUM-001', 'PAT-NUM-001', 'ACC001', '张三-中心医院账户', 'A');
MERGE INTO dml_patient_account (account_num, patient_num, account_number, account_name, status) KEY(account_num)
VALUES ('ACCT-NUM-002', 'PAT-NUM-002', 'ACC002', '李四-中心医院账户', 'A');
MERGE INTO dml_patient_account (account_num, patient_num, account_number, account_name, status) KEY(account_num)
VALUES ('ACCT-NUM-003', 'PAT-NUM-003', 'ACC003', '王五-中心医院账户', 'A');
MERGE INTO dml_patient_account (account_num, patient_num, account_number, account_name, status) KEY(account_num)
VALUES ('ACCT-NUM-004', 'PAT-NUM-004', 'ACC004', '孙六-中心医院账户', 'I');

-- ----------------------------------------------------------------------------
-- 14. 患者就诊记录 (对应 novanet.patient_visits)
--     记录患者入院就诊的科室/床位/主治医生等信息
--     visit_type(patient_class): I=住院, E=急诊, O=门诊
--     status: A=活跃就诊, I=已出院
-- ----------------------------------------------------------------------------
-- 张三 ICU 住院就诊
MERGE INTO dml_patient_visit (visit_num, visit_number, patient_num, account_num, visit_type, location, room, bed, facility, admitting_doctor, visit_date, status) KEY(visit_num)
VALUES ('VISIT-NUM-001', 'V2026001', 'PAT-NUM-001', 'ACCT-NUM-001', 'I', 'ICU重症监护室', '101', 'A', '中心医院', 'Gregory House', '2026-01-15 09:30:00', 'A');
-- 李四 急诊就诊
MERGE INTO dml_patient_visit (visit_num, visit_number, patient_num, account_num, visit_type, location, room, bed, facility, admitting_doctor, visit_date, status) KEY(visit_num)
VALUES ('VISIT-NUM-002', 'V2026002', 'PAT-NUM-002', 'ACCT-NUM-002', 'E', '急诊科', '201', 'B', '中心医院', 'Lisa Cuddy', '2026-01-20 14:15:00', 'A');
-- 王五 普外科住院就诊
MERGE INTO dml_patient_visit (visit_num, visit_number, patient_num, account_num, visit_type, location, room, bed, facility, admitting_doctor, visit_date, status) KEY(visit_num)
VALUES ('VISIT-NUM-003', 'V2026003', 'PAT-NUM-003', 'ACCT-NUM-003', 'I', '普外科', '301', 'C', '中心医院', 'James Wilson', '2026-01-25 10:00:00', 'A');
-- 孙六 内科就诊 (已出院)
MERGE INTO dml_patient_visit (visit_num, visit_number, patient_num, account_num, visit_type, location, room, bed, facility, admitting_doctor, visit_date, discharging_date, status) KEY(visit_num)
VALUES ('VISIT-NUM-004', 'V2026004', 'PAT-NUM-004', 'ACCT-NUM-004', 'I', '内科', '401', 'D', '中心医院', 'Gregory House', '2026-01-10 08:00:00', '2026-01-28 16:00:00', 'I');

-- ----------------------------------------------------------------------------
-- 15. 仪器设备 (对应 novanet.instruments)
--     床旁检测设备实例, 关联科室和设备类型
-- ----------------------------------------------------------------------------
MERGE INTO dml_device (serial_id, device_name, device_type, device_class, from_inst_id, sw_version, loc_num, fac_name, inst_type, supports_set_time, supports_continuous) KEY(serial_id)
VALUES ('SS-001', 'StatStrip-ICU-01', 'StatStrip', 'POCT', 'SS-001', '3.2.1', 'LOC-ICU-001', '中心医院', 'StatStrip', TRUE, FALSE);
MERGE INTO dml_device (serial_id, device_name, device_type, device_class, from_inst_id, sw_version, loc_num, fac_name, inst_type, supports_set_time, supports_continuous) KEY(serial_id)
VALUES ('SS-002', 'StatStrip-ER-01', 'StatStrip', 'POCT', 'SS-002', '3.2.1', 'LOC-ER-001', '中心医院', 'StatStrip', TRUE, FALSE);
MERGE INTO dml_device (serial_id, device_name, device_type, device_class, from_inst_id, sw_version, loc_num, fac_name, inst_type, supports_set_time, supports_continuous) KEY(serial_id)
VALUES ('SS-003', 'StatStrip-GS-01', 'StatStrip', 'POCT', 'SS-003', '3.2.1', 'LOC-GS-001', '中心医院', 'StatStrip', TRUE, FALSE);

-- ----------------------------------------------------------------------------
-- 16. 通信配置 (对应 novanet.communications)
--     DML 协议服务端口配置 - 接收设备数据/推送列表
-- ----------------------------------------------------------------------------
MERGE INTO dml_communication (computer_name, instrument_id, port_num, port_type, comm_record_num, started_dttm) KEY(comm_record_num)
VALUES ('NOVA-SERVER', 'SS-001', 57381, 'DML', 'COMM-REC-001', '2026-01-01 00:00:00');
MERGE INTO dml_communication (computer_name, instrument_id, port_num, port_type, comm_record_num, started_dttm) KEY(comm_record_num)
VALUES ('NOVA-SERVER', 'SS-002', 57381, 'DML', 'COMM-REC-002', '2026-01-01 00:00:00');
MERGE INTO dml_communication (computer_name, instrument_id, port_num, port_type, comm_record_num, started_dttm) KEY(comm_record_num)
VALUES ('NOVA-SERVER', 'SS-003', 57381, 'DML', 'COMM-REC-003', '2026-01-01 00:00:00');

-- ----------------------------------------------------------------------------
-- 17. 健康状态记录 (对应 novanet.health_ping)
--     RTM 服务运行状态心跳
-- ----------------------------------------------------------------------------
MERGE INTO dml_health_ping (process_name, host, update_time, last_connect_dttm, tot_messages_processed) KEY(process_name, host)
VALUES ('RTMADTP', 'NOVA-SERVER', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);
MERGE INTO dml_health_ping (process_name, host, update_time, last_connect_dttm, tot_messages_processed) KEY(process_name, host)
VALUES ('RTMOPL', 'NOVA-SERVER', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);
MERGE INTO dml_health_ping (process_name, host, update_time, last_connect_dttm, tot_messages_processed) KEY(process_name, host)
VALUES ('RTMLIS', 'NOVA-SERVER', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);
