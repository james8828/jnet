-- ============================================================================
-- DML Protocol Server - Initial Seed Data
-- ============================================================================

-- Default location hierarchy (facility + location)
MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, is_default, status) KEY(loc_num)
VALUES ('LOC001', 'Default Facility', NULL, 1, 'Default Facility', 'T', 'Active');

MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, is_default, status) KEY(loc_num)
VALUES ('LOC002', 'ICU', 'LOC001', 2, 'Default Facility', 'T', 'Active');

MERGE INTO dml_location (loc_num, loc_name, parent_loc_num, level_num, facility, is_default, status) KEY(loc_num)
VALUES ('LOC003', 'ER', 'LOC001', 2, 'Default Facility', 'F', 'Active');

-- Default loc_last_update entries
MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'SETUP', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'WIFI_SETUP', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'LOTS', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'OPERATORS', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'PATIENTS', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'PHYSICIANS', '2000-01-01 00:00:00');

MERGE INTO dml_loc_last_update (loc_num, data_type, last_update_time) KEY(loc_num, data_type)
VALUES ('LOC002', 'LOCATIONS', '2000-01-01 00:00:00');

-- Default config data for setup
MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG001', 'accn_id_max_length*V', '16');

MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG002', 'accn_id_min_length*V', '1');

MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG003', 'patient_id_max_length*V', '20');

MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG004', 'operator_id_max_length*V', '20');

MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG005', 'allow_testing_default_unit*V', '1');

MERGE INTO dml_config_data (config_num, config_key, config_value) KEY(config_num)
VALUES ('CFG006', 'obs_review_no_login*V', '1');

-- Link config to location
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG001', 'StatStrip');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG002', 'StatStrip');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG003', 'StatStrip');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG004', 'StatStrip');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG005', 'StatStrip');
MERGE INTO dml_loc_to_config (loc_num, config_num, inst_type) KEY(loc_num, config_num)
VALUES ('LOC002', 'CFG006', 'StatStrip');

-- Default instrument tests
MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Glu', 'Glu', '2341-6', 'NM', 'mg/dL', '80', '120', 1);

MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Ket', 'Ket', '3394-6', 'NM', 'mmol/L', '0', '6', 2);

MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Lac', 'Lac', '3261-5', 'NM', 'mmol/L', '0', '10', 3);

MERGE INTO dml_instrument_test (inst_type, test_name, generic_test_name, test_code, result_type_code, units, lo_limit, hi_limit, ui_order) KEY(inst_type, test_name)
VALUES ('StatStrip', 'Hct', 'Hct', '20570-8', 'NM', '%', '20', '60', 4);

-- Default test ranges
MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Glu', 'StatStrip', 'mg/dL', '1', '0');

MERGE INTO dml_test_offset (generic_test_name, inst_type, units, slope, intercept) KEY(generic_test_name, inst_type, units)
VALUES ('Ket', 'StatStrip', 'mmol/L', '1', '0');

-- Default reagent lot
MERGE INTO dml_lot (lots_key_num, lot, lot_type, lot_name, exp_date, in_use, used_count, retired, is_validated) KEY(lots_key_num)
VALUES ('LOT_KEY_001', 'LOT001', 'TestStrip', 'TestStrip Lot 1', '2027-12-31', 'T', 0, 'F', 'F');

MERGE INTO dml_lot (lots_key_num, lot, lot_type, lot_name, exp_date, in_use, used_count, retired, is_validated) KEY(lots_key_num)
VALUES ('LOT_KEY_002', 'LOT002', 'Control', 'QC Level 1', '2027-12-31', 'T', 0, 'F', 'F');

-- Link lot to device type
MERGE INTO dml_device_to_lot (lots_key_num, inst_type) KEY(lots_key_num, inst_type)
VALUES ('LOT_KEY_001', 'StatStrip');
MERGE INTO dml_device_to_lot (lots_key_num, inst_type) KEY(lots_key_num, inst_type)
VALUES ('LOT_KEY_002', 'StatStrip');

-- Link lot to location
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT_KEY_001', 'LOC002');
MERGE INTO dml_lot_to_unit (lots_key_num, loc_num) KEY(lots_key_num, loc_num)
VALUES ('LOT_KEY_002', 'LOC002');

-- Lot chemistry ranges
MERGE INTO dml_lot_chem (lots_key_num, generic_test_name, test_name, observation_id, lo_limit, hi_limit, units, level_number, level_type) KEY(lots_key_num, generic_test_name, units, level_number)
VALUES ('LOT_KEY_002', 'Glu', 'Glu', '2341-6', '70', '130', 'mg/dL', '1', 'Control');

-- Default physician
MERGE INTO dml_physician (physician_id, physician_name, first_name, last_name, prefix, suffix, status) KEY(physician_id)
VALUES ('PHY001', 'Gregory House', 'Gregory', 'House', 'Dr', 'MD', 'Active');

-- Default operator
MERGE INTO dml_operator (operator_id, operator_name, first_name, last_name, access_control_level, status) KEY(operator_id)
VALUES ('OP001', 'Admin User', 'Admin', 'User', 16, 'Active');

MERGE INTO dml_operator (operator_id, operator_name, first_name, last_name, access_control_level, status) KEY(operator_id)
VALUES ('OP002', 'Test Operator', 'Test', 'Operator', 8, 'Active');
