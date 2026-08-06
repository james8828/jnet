-- ============================================================================
-- DML Protocol Server - Database Schema
-- Based on C# DMLProtocol.cs SQL queries and NOVANET database structure
-- Compatible with H2 (development) and PostgreSQL (production)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Device table (extends dml_device for additional protocol fields)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_id VARCHAR(64) UNIQUE NOT NULL,
    device_name VARCHAR(128),
    device_type VARCHAR(64),
    device_class VARCHAR(64),
    from_inst_id VARCHAR(64),
    vendor_id VARCHAR(64),
    sw_version VARCHAR(32),
    hw_version VARCHAR(32),
    loc_num VARCHAR(64),
    fac_name VARCHAR(128),
    inst_num VARCHAR(64),
    inst_type VARCHAR(64),
    supports_set_time BOOLEAN DEFAULT FALSE,
    supports_continuous BOOLEAN DEFAULT FALSE,
    is_continuous BOOLEAN DEFAULT FALSE,
    always_send BOOLEAN DEFAULT FALSE,
    last_comm_dttm TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Locations (facility/location hierarchy)
-- Mirrors C# DBA.inst_locations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num VARCHAR(50) UNIQUE NOT NULL,
    loc_name VARCHAR(200),
    parent_loc_num VARCHAR(50),
    level_num INT,
    facility VARCHAR(200),
    description VARCHAR(500),
    is_default VARCHAR(5) DEFAULT 'F',
    inst_class VARCHAR(64),
    inst_type VARCHAR(64),
    status VARCHAR(20) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Location last update tracking (mirrors C# DBA.loc_last_update)
-- Tracks when each data type was last updated per location
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_loc_last_update (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    inst_class VARCHAR(64),
    inst_type VARCHAR(64),
    CONSTRAINT uk_loc_last_update UNIQUE (loc_num, data_type, inst_class, inst_type)
);

-- ----------------------------------------------------------------------------
-- Operators (mirrors C# DBA.operators)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_operator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id VARCHAR(100) UNIQUE NOT NULL,
    operator_name VARCHAR(200),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    access_control_level INT,
    privilege_level INT,
    facility VARCHAR(200),
    location VARCHAR(200),
    loc_num VARCHAR(50),
    department VARCHAR(200),
    effective_start_dttm TIMESTAMP,
    effective_end_dttm TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Active',
    note VARCHAR(1000),
    datetime_stamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Patients (mirrors C# DBA.patients)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_patient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(100) UNIQUE NOT NULL,
    medrec_num VARCHAR(100),
    enterprise_id VARCHAR(100),
    account_num VARCHAR(100),
    patient_name VARCHAR(200),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    birth_date DATE,
    sex VARCHAR(5),
    race VARCHAR(50),
    facility VARCHAR(200),
    location VARCHAR(200),
    loc_num VARCHAR(50),
    bed VARCHAR(50),
    room VARCHAR(50),
    diagnosis_code VARCHAR(100),
    diagnosis_desc VARCHAR(500),
    physician_id VARCHAR(100),
    physician_name VARCHAR(200),
    status VARCHAR(20) DEFAULT 'Active',
    note VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Physicians (mirrors C# DBA.physicians)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_physician (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    physician_id VARCHAR(100) UNIQUE NOT NULL,
    physician_name VARCHAR(200),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    prefix VARCHAR(20),
    suffix VARCHAR(20),
    facility VARCHAR(200),
    location VARCHAR(200),
    loc_num VARCHAR(50),
    department VARCHAR(200),
    status VARCHAR(20) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Samples (mirrors C# DBA.samples)
-- Stores observation/sample data from devices
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_sample_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_key_num VARCHAR(100) UNIQUE,
    device_serial_id VARCHAR(100),
    sample_date TIMESTAMP,
    transmitted_flag VARCHAR(10) DEFAULT 'F',
    saved_to_history_db_flag VARCHAR(10) DEFAULT 'F',
    control_type VARCHAR(50),
    accession_num VARCHAR(100),
    control_lot_num VARCHAR(100),
    strip_lot_num VARCHAR(100),
    lot_level VARCHAR(50),
    internal_external VARCHAR(20),
    patient_id VARCHAR(100),
    medrec_num VARCHAR(100),
    account_num VARCHAR(100),
    enterprise_id VARCHAR(100),
    loc_num VARCHAR(50),
    loc_name VARCHAR(200),
    fac_name VARCHAR(200),
    device_type VARCHAR(64),
    device_name VARCHAR(128),
    device_sw_ver VARCHAR(32),
    xml_text TEXT,
    is_qc BOOLEAN DEFAULT FALSE,
    sample_id_type VARCHAR(20),
    observation_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Observations (individual test results within a sample)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_observation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    sample_key_num VARCHAR(64),
    accession_num VARCHAR(64),
    patient_id VARCHAR(64),
    mrn VARCHAR(64),
    account_num VARCHAR(64),
    test_cd VARCHAR(64),
    test_name VARCHAR(128),
    result_value VARCHAR(128),
    result_units VARCHAR(32),
    result_flags VARCHAR(16),
    interpretation_cd VARCHAR(16),
    normal_lo_limit VARCHAR(32),
    normal_hi_limit VARCHAR(32),
    critical_lo_limit VARCHAR(32),
    critical_hi_limit VARCHAR(32),
    control_type VARCHAR(32),
    control_lot_num VARCHAR(64),
    strip_lot_num VARCHAR(64),
    observation_dttm TIMESTAMP,
    xml_text TEXT,
    transmitted_flag VARCHAR(10) DEFAULT 'F',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Device Events (mirrors C# DBA.device_events)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_device_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(10),
    event_type_cd VARCHAR(50),
    event_dttm TIMESTAMP,
    event_desc VARCHAR(500),
    severity_cd VARCHAR(5),
    event_code VARCHAR(100),
    event_status VARCHAR(20),
    device_serial_id VARCHAR(100),
    inst_num VARCHAR(64),
    operator_id VARCHAR(100),
    operator_name VARCHAR(200),
    facility VARCHAR(200),
    location VARCHAR(200),
    arch VARCHAR(5) DEFAULT 'F',
    uuid VARCHAR(64),
    xml_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Reagent Lots (mirrors C# DBA.lots)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_lot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lots_key_num VARCHAR(100) UNIQUE,
    lot VARCHAR(100),
    lot_type VARCHAR(50),
    lot_name VARCHAR(100),
    exp_date DATE,
    datetime_stamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    in_use VARCHAR(5) DEFAULT 'T',
    used_count INT DEFAULT 0,
    retired VARCHAR(5) DEFAULT 'F',
    is_validated VARCHAR(5) DEFAULT 'F',
    level_cd VARCHAR(20),
    level_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Lot Chemistry (mirrors C# DBA.lot_chem)
-- Stores chemistry/range data per lot
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_lot_chem (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lots_key_num VARCHAR(100),
    generic_test_name VARCHAR(100),
    test_name VARCHAR(100),
    observation_id VARCHAR(100),
    lo_limit VARCHAR(32),
    hi_limit VARCHAR(32),
    units VARCHAR(32),
    facility_num VARCHAR(50),
    level_number VARCHAR(20),
    level_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Device to Lot mapping (mirrors C# DBA.device_to_lot)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_device_to_lot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lots_key_num VARCHAR(100),
    inst_type VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Lots to Unit mapping (mirrors C# DBA.lots_to_unit)
-- Maps lots to locations/units
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_lot_to_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lots_key_num VARCHAR(100),
    loc_num VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Configuration Data (mirrors C# DBA.config_data)
-- Key-value configuration for device setup
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_config_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_num VARCHAR(100) UNIQUE,
    config_key VARCHAR(200),
    config_value VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Location to Config mapping (mirrors C# DBA.loc_to_config)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_loc_to_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num VARCHAR(50),
    config_num VARCHAR(100),
    inst_type VARCHAR(64),
    inst_class VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- WiFi Setup (mirrors C# DBA.wifi_setup)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_wifi_setup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id VARCHAR(100) UNIQUE,
    wifi_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Location to WiFi Setup mapping (mirrors C# DBA.loc_to_wifi_setup)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_loc_to_wifi_setup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num VARCHAR(50),
    inst_class VARCHAR(64),
    config_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- WiFi Credentials (mirrors C# DBA.wifi_credentials)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_wifi_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fac_num VARCHAR(50),
    loc_num VARCHAR(50),
    wifi_mac_address VARCHAR(100),
    wifi_user_name VARCHAR(200),
    wifi_password VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Firmware (mirrors C# DBA.firmware)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_firmware (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    firmware_id VARCHAR(100) UNIQUE,
    device_type VARCHAR(64),
    device_class VARCHAR(64),
    major_version INT,
    minor_version INT,
    build_num INT,
    revision INT,
    language_code VARCHAR(10),
    region VARCHAR(10),
    firmware_data TEXT,
    file_name VARCHAR(200),
    release_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Orders (mirrors C# DBA.orders)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    accession_num VARCHAR(100) UNIQUE,
    order_id VARCHAR(100),
    patient_id VARCHAR(100),
    device_serial_id VARCHAR(100),
    loc_num VARCHAR(50),
    facility VARCHAR(200),
    location VARCHAR(200),
    ordering_provider_id VARCHAR(100),
    universal_service_id VARCHAR(100),
    order_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Operator Messages (mirrors C# DBA.operator_message)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_operator_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num VARCHAR(100),
    msg_create_dttm TIMESTAMP,
    msg_read_dttm TIMESTAMP,
    msg_priority INT DEFAULT 0,
    current_msg VARCHAR(5) DEFAULT 'F',
    msg_text VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Instruments Tests (mirrors C# DBA.instruments_tests)
-- Test configuration per instrument type
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_instrument_test (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inst_type VARCHAR(64),
    inst_class VARCHAR(64),
    test_name VARCHAR(100),
    generic_test_name VARCHAR(100),
    test_code VARCHAR(64),
    test_code_system VARCHAR(32),
    result_type_code VARCHAR(10),
    units VARCHAR(32),
    units_of_measure VARCHAR(32),
    lo_limit VARCHAR(32),
    hi_limit VARCHAR(32),
    lo_panic_limit VARCHAR(32),
    hi_panic_limit VARCHAR(32),
    lo_normal_limit VARCHAR(32),
    hi_normal_limit VARCHAR(32),
    sex VARCHAR(5),
    age_type VARCHAR(10),
    age_lo INT,
    age_hi INT,
    enable_all_ages VARCHAR(5) DEFAULT 'T',
    range_label VARCHAR(100),
    equation VARCHAR(200),
    eq_const VARCHAR(100),
    enable_deselect VARCHAR(5) DEFAULT 'T',
    ui_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Test Offsets (mirrors C# DBA.test_offsets)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_test_offset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generic_test_name VARCHAR(100),
    inst_type VARCHAR(64),
    inst_class VARCHAR(64),
    units VARCHAR(32),
    slope VARCHAR(32),
    intercept VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Facility Test Units (mirrors C# DBA.facility_test_units)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_facility_test_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loc_num VARCHAR(50),
    generic_test_name VARCHAR(100),
    units_of_measure VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Reagent (mirrors C# DBA.reagent - simplified reagent catalog)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_reagent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reagent_num VARCHAR(50),
    reagent_name VARCHAR(200),
    reagent_type VARCHAR(20),
    lot_number VARCHAR(100),
    level_cd VARCHAR(20),
    valid_start_date DATE,
    valid_end_date DATE,
    facility VARCHAR(200),
    location VARCHAR(200),
    inst_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'Active',
    note VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Communications audit log (mirrors C# DBA.communications)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_communication (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    computer_name VARCHAR(100),
    instrument_id VARCHAR(100),
    port_num INT,
    comm_record_num VARCHAR(100),
    started_dttm TIMESTAMP,
    last_comm_dttm TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Indexes for performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_sample_device ON dml_sample_data(device_serial_id);
CREATE INDEX IF NOT EXISTS idx_sample_date ON dml_sample_data(sample_date);
CREATE INDEX IF NOT EXISTS idx_sample_accession ON dml_sample_data(accession_num);
CREATE INDEX IF NOT EXISTS idx_sample_patient ON dml_sample_data(patient_id);
CREATE INDEX IF NOT EXISTS idx_obs_device ON dml_observation(device_id);
CREATE INDEX IF NOT EXISTS idx_obs_sample ON dml_observation(sample_key_num);
CREATE INDEX IF NOT EXISTS idx_obs_created ON dml_observation(created_at);
CREATE INDEX IF NOT EXISTS idx_event_device ON dml_device_event(device_serial_id);
CREATE INDEX IF NOT EXISTS idx_event_dttm ON dml_device_event(event_dttm);
CREATE INDEX IF NOT EXISTS idx_loc_parent ON dml_location(parent_loc_num);
CREATE INDEX IF NOT EXISTS idx_lot_type ON dml_lot(lot_type);
CREATE INDEX IF NOT EXISTS idx_lotchem_lot ON dml_lot_chem(lots_key_num);
CREATE INDEX IF NOT EXISTS idx_dev2lot_lot ON dml_device_to_lot(lots_key_num);
CREATE INDEX IF NOT EXISTS idx_lot2unit_loc ON dml_lot_to_unit(loc_num);
CREATE INDEX IF NOT EXISTS idx_loc2config_loc ON dml_loc_to_config(loc_num);
CREATE INDEX IF NOT EXISTS idx_loc2wifi_loc ON dml_loc_to_wifi_setup(loc_num);
CREATE INDEX IF NOT EXISTS idx_wificred_mac ON dml_wifi_credential(wifi_mac_address);
CREATE INDEX IF NOT EXISTS idx_insttest_type ON dml_instrument_test(inst_type);
CREATE INDEX IF NOT EXISTS idx_insttest_testname ON dml_instrument_test(test_name);

-- ============================================================================
-- RTM Extension Tables (RTMADTP, RTMOPL, RTMLIS)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Patient table (RTMADTP)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_patient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_num VARCHAR(100) UNIQUE,
    patient_id VARCHAR(100),
    medrec_num VARCHAR(100),
    account_num VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    birth_date DATE,
    sex VARCHAR(5),
    race VARCHAR(50),
    address VARCHAR(500),
    phone_home VARCHAR(40),
    facility VARCHAR(200),
    location VARCHAR(200),
    status VARCHAR(20) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Patient account table (RTMADTP)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_patient_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_num VARCHAR(100) UNIQUE,
    patient_num VARCHAR(100),
    account_number VARCHAR(100),
    account_name VARCHAR(200),
    status VARCHAR(20) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Patient visit table (RTMADTP)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_patient_visit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_num VARCHAR(100) UNIQUE,
    visit_number VARCHAR(100),
    patient_num VARCHAR(100),
    account_num VARCHAR(100),
    visit_type VARCHAR(50),
    location VARCHAR(200),
    room VARCHAR(50),
    bed VARCHAR(50),
    facility VARCHAR(200),
    admitting_doctor VARCHAR(200),
    visit_date TIMESTAMP,
    discharging_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Operator privilege table (RTMOPL)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_operator_privilege (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num VARCHAR(100),
    inst_type VARCHAR(64),
    privilege_code VARCHAR(50),
    privilege_desc VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Operator to unit table (RTMOPL)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_operator_to_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num VARCHAR(100),
    loc_num VARCHAR(50),
    unit_name VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Method table (RTMOPL)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_method (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_num VARCHAR(100),
    inst_type VARCHAR(64),
    method_name VARCHAR(100),
    method_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- QC result table (RTMLIS)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_qc_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_key_num VARCHAR(100),
    lot_number VARCHAR(100),
    control_type VARCHAR(50),
    test_code VARCHAR(64),
    result_value VARCHAR(128),
    result_units VARCHAR(32),
    target_value VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Health ping table (RTM monitoring)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dml_health_ping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_name VARCHAR(40),
    host VARCHAR(100),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_connect_dttm TIMESTAMP,
    last_disconnect_dttm TIMESTAMP,
    tot_messages_processed INT DEFAULT 0
);

-- Additional RTM indexes
CREATE INDEX IF NOT EXISTS idx_patient_medrec ON dml_patient(medrec_num);
CREATE INDEX IF NOT EXISTS idx_patient_id ON dml_patient(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_account ON dml_patient_account(patient_num);
CREATE INDEX IF NOT EXISTS idx_visit_patient ON dml_patient_visit(patient_num);
CREATE INDEX IF NOT EXISTS idx_visit_number ON dml_patient_visit(visit_number);
CREATE INDEX IF NOT EXISTS idx_op_privilege ON dml_operator_privilege(operator_num);
CREATE INDEX IF NOT EXISTS idx_op_unit ON dml_operator_to_unit(operator_num);
CREATE INDEX IF NOT EXISTS idx_op_method ON dml_method(operator_num);
CREATE INDEX IF NOT EXISTS idx_qc_sample ON dml_qc_result(sample_key_num);
CREATE INDEX IF NOT EXISTS idx_qc_lot ON dml_qc_result(lot_number);
CREATE INDEX IF NOT EXISTS idx_health_process ON dml_health_ping(process_name);
