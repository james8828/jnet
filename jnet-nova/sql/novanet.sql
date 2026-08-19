create table COMM_AUDIT
(
    Computer_Name  char(200)     default '' not null,
    Instrument_ID  char(144)                not null
        constraint Inst_id_index
            unique,
    Event_DateTime timestamp(23) default timestamp not null,
    Event_Code     char(16)                 not null,
    Event_Desc     char(1000),
    Event_Param    char(20),
    locked_by      char(320),
    constraint ASA107
        primary key (Computer_Name, Instrument_ID, Event_DateTime, Event_Code)
);

comment
on column COMM_AUDIT.Computer_Name is 'This is the name of the computer that the ICP (that sent the result) is running on';

comment
on column COMM_AUDIT.Instrument_ID is 'This is the Serial number or ID of the instrument that generated the event';

comment
on column COMM_AUDIT.Event_DateTime is 'This is the time that the communications event with the instrument was completed.';

comment
on column COMM_AUDIT.Event_Code is '
	''1''=Results received from instrument,
	''2''=Device Events received,
	''3''=Operator list sent to instrument,
	''4''=Patient list sent,
	''5''=Location list sent,
	''6''=Physician list sent,
	''7''=Reagents sent,
	''8''=Setup sent,
	''9''=Firmware update sent,
	''10''=Connect,
	''11''=Disconnect';

comment
on column COMM_AUDIT.Event_Desc is 'The communication event description';

comment
on column COMM_AUDIT.Event_Param is 'Parameter of the event. # (of results) if Event_Code=''1'' or ''2'', ''I''(incremental) or ''C''(complete) if Event_Code=''3'' or ''4'' or ''6'', ''N''(normal) or ''A''(abnormal) if Event_Code=''11''';

create table Communications
(
    Computer_Name    char(200)     default '' not null,
    Instrument_ID    char(144)                not null,
    Protocol         char(12)                 not null,
    Port_Type        char(12)                 not null,
    Comm_Protocol    char(16)                 not null,
    Port_Num         int(10) default 0 not null,
    Baud             char(6),
    Data_Bits        char(1),
    Stop_Bits        char(1),
    Parity           char(1),
    Flow_Control     int(10) default 0 not null,
    Run_Mode         int(10),
    Connect_Remote   int(10) default 0 not null,
    IP_Address       int(10) default 0,
    Remote_Host_Name char(200)     default '',
    Port_Active      int(10) default 0 not null,
    Remote_Port      int(10) default 0,
    Rcv_Application  char(200),
    Rcv_Facility     char(200),
    Last_Activity    unsigned int(10) default 0 not null,
    Last_Status      int(10) default 0,
    InstrumentUUID   char(36),
    Used             char(1),
    Multi_Connect    char(1),
    datetime_stamp   timestamp(23) default timestamp,
    locked_by        char(320),
    comm_record_num  char(36)                 not null
        constraint ASA87
            primary key,
    from_ui          char(1),
    from_inst_id     char(144),
    remote_host_by   char(20),
    constraint "Communications UNIQUE (Computer_Name,Instrument_ID,Port_Num)"
        unique (Computer_Name, Instrument_ID, Port_Num)
);

comment
on column Communications.Computer_Name is 'Host Name to which instrument is connected';

comment
on column Communications.Instrument_ID is 'Identifier from the instrument';

comment
on column Communications.Protocol is 'The Protocol that is used by the instrument for communication (i.e. ASTM, HL7, etc)';

comment
on column Communications.Port_Type is 'Port Configuration Type (i.e. Analyzer, LIS, etc)';

comment
on column Communications.Comm_Protocol is 'Communication Protocol (i.e. Serial, TCPIP,IPX,etc)';

comment
on column Communications.Port_Num is 'The port number defining the Serial Port, Local Socket Port, etc.  Dependent upon the Comm_Protocol';

comment
on column Communications.Baud is 'For serial ports, baud rate';

comment
on column Communications.Data_Bits is 'For serial ports, number of data bits';

comment
on column Communications.Stop_Bits is 'For Serial ports, number of stop bits';

comment
on column Communications.Parity is 'For serial ports, parity mode';

comment
on column Communications.Flow_Control is 'For serial ports, flow control method';

comment
on column Communications.Run_Mode is '"P"roduction, "T"raining, "D"ebug';

comment
on column Communications.Connect_Remote is 'For Socket communications, must connect to remote port or accept connections from remote';

comment
on column Communications.IP_Address is 'For Socket Communications, IP Address';

comment
on column Communications.Remote_Host_Name is 'For Socket Communication, remote host name (DNS Name)';

comment
on column Communications.Port_Active is 'For Socket Communications, Port is active';

comment
on column Communications.Remote_Port is 'For Socket Communications, Port on remote host to connect';

comment
on column Communications.Rcv_Application is 'Receiving Application';

comment
on column Communications.Rcv_Facility is 'Receiving Facility';

comment
on column Communications.Last_Activity is 'Last Activity Stamp';

comment
on column Communications.Last_Status is 'Last Known Status';

comment
on column Communications.InstrumentUUID is 'Instrument Number';

comment
on column Communications.Used is 'Connection used flag';

comment
on column Communications.Multi_Connect is 'Multiple connections acceptor flag';

comment
on column Communications.comm_record_num is 'UID of this record';

comment
on column Communications.from_ui is '''T'' if record was created by UI, ''F'' or NULL if not';

comment
on column Communications.from_inst_id is 'Instrument ID of template (multi-connect) port';

comment
on column Communications.remote_host_by is 'The column name of entering remote host from UI. "IP_Address", "Remote_Host_Name" or NULL.';

create table DownloadFirmware
(
    inst_type        char(20) not null,
    firmware_version char(40) not null,
    index_num        int(10) not null,
    firmware_data    varchar(1024),
    constraint ASA112
        primary key (inst_type, firmware_version, index_num)
);

comment
on table DownloadFirmware is 'This is where the firware data be kept for applying to instruments';

comment
on column DownloadFirmware.inst_type is 'The instrument type to which the firmware applies';

comment
on column DownloadFirmware.firmware_version is 'The version number of the firmware';

comment
on column DownloadFirmware.index_num is 'The index of data block for this version of firmware';

comment
on column DownloadFirmware.firmware_data is 'A data block of firmware';

create table EXT_USER
(
    user_name varchar(64) not null
        constraint ASA122
            primary key,
    password  varchar(36) not null,
    cookie    varchar(10) not null
);

create table ErrorMessages
(
    RcvDateTime       char(14) not null,
    Computer_Name     char(200),
    Instrument_ID     char(144),
    patient_uuid      char(36),
    Sample_num        char(6),
    sample_type_code  char(12),
    Accession_num     char(84),
    SampleDateTime    char(14),
    sample_key_num    char(36),
    ErrorCode         smallint(5),
    ErrorMessage      char(1000),
    Port_num          char(50),
    Last_state_num    int(10),
    Last_state        char(50),
    Last_function_num int(10),
    Last_function     char(50),
    Facility          char(128),
    loc_name          char(128),
    loc_num           char(36),
    Severity          smallint(5),
    Processed         smallint(5),
    err_source_num    int(10),
    err_source        char(50),
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    arch              char(1),
    err_msg_num       char(36) not null
        constraint ASA89
            primary key
);

comment
on column ErrorMessages.RcvDateTime is 'Message received date/time';

comment
on column ErrorMessages.Computer_Name is 'Host Name to which instrument was connected';

comment
on column ErrorMessages.Instrument_ID is 'Identifier from the instrument';

comment
on column ErrorMessages.patient_uuid is 'Patient Key Number';

comment
on column ErrorMessages.Sample_num is 'Sample Number';

comment
on column ErrorMessages.sample_type_code is 'LOINC code for sample type';

comment
on column ErrorMessages.Accession_num is 'Accession number';

comment
on column ErrorMessages.SampleDateTime is 'Sample or Specimen date/time';

comment
on column ErrorMessages.sample_key_num is 'Sample Key Number';

comment
on column ErrorMessages.ErrorCode is 'Error code';

comment
on column ErrorMessages.ErrorMessage is 'Error Message';

comment
on column ErrorMessages.Port_num is 'port number associated with device error (may include IP address and socket number)';

comment
on column ErrorMessages.Last_state_num is 'string number of last state of protocol associated with device';

comment
on column ErrorMessages.Last_state is 'last state of protocol associated with device';

comment
on column ErrorMessages.Last_function_num is 'string number of last function of protocol associated with device';

comment
on column ErrorMessages.Last_function is 'last function of protocol associated with device';

comment
on column ErrorMessages.Facility is 'name of facility associated with error ("*" for all facilities)';

comment
on column ErrorMessages.loc_name is 'name of location associated with error ("*" for all locations)';

comment
on column ErrorMessages.loc_num is 'UUID of location associated with error (UUID of facility if all locations, blank if all facilities)';

comment
on column ErrorMessages.Severity is '1 = infomation, 2 = warning, 3 = error, 4 = call Nova';

comment
on column ErrorMessages.Processed is '0 = not processed, 1 = processed';

comment
on column ErrorMessages.err_source_num is 'The string number for the source of the error';

comment
on column ErrorMessages.err_source is 'The string (in U.S. English) for the source of the error ("Instrument Control", "Rtm Manager", "LIS")';

comment
on column ErrorMessages.arch is '''T'' during the time this error record is in the process of being archived,''F'' if not';

comment
on column ErrorMessages.err_msg_num is 'UID for this record';

create table FIRMWARE
(
    firmware_path    char(1024),
    datetime_stamp   timestamp(23) default timestamp,
    locked_by        char(320),
    firmware_name    char(255),
    firmware_num     char(36) not null
        constraint ASA98
            primary key,
    inst_type        char(20),
    firmware_version char(40)
);

comment
on column FIRMWARE.firmware_path is 'The fully qualified file path name of the firmware related to this configuration';

comment
on column FIRMWARE.firmware_name is 'File name w/o path or extension';

comment
on column FIRMWARE.firmware_num is 'UUID for this row';

comment
on column FIRMWARE.firmware_version is 'The version number of the firmware';

create table INSTRUMENT_TYPES
(
    inst_type          char(20) not null
        constraint ASA80
            primary key,
    does_remote_review char(1),
    inst_class         char(12),
    datetime_stamp     timestamp(23) default timestamp,
    locked_by          char(320),
    use_inst_lot_data  char(1)
);

comment
on column INSTRUMENT_TYPES.inst_class is 'Instrument class type (i.e. Analyzer, ADT, LIS or Oper List)';

comment
on column INSTRUMENT_TYPES.use_inst_lot_data is '''T'' if using the data of INST_LOT_DATA table, ''F'' if not';

create table INSTRUMENTS_TESTS
(
    inst_type             char(20) not null
        constraint instrument_types
            references INSTRUMENT_TYPES,
    test_code             char(30) not null,
    sample_type_code      char(12),
    result_type_code      char(1),
    units                 char(80),
    order_calc_flag       char(1),
    datetime_stamp        timestamp(23) default timestamp,
    locked_by             char(320),
    lo_limit              char(10) not null,
    hi_limit              char(10) not null,
    resolution            char(10) not null,
    test_name             char(30),
    generic_test_name     char(30),
    valid_sar_test        char(1),
    instruments_tests_num char(36) not null
        constraint ASA79
            primary key,
    test_transmit_name    char(30),
    send_to_inst          char(1),
    test_code_system      char(80)
);

comment
on column INSTRUMENTS_TESTS.test_code is 'LOINC code for test name';

comment
on column INSTRUMENTS_TESTS.sample_type_code is 'LOINC code for sample type';

comment
on column INSTRUMENTS_TESTS.result_type_code is 'LOINC code for result type. ''C''=Calculated,''D''=Default,''E''=Estimated,''I''=Input,''M''=Measured,''U''=Unknown';

comment
on column INSTRUMENTS_TESTS.test_name is 'Name of test from instrument if not loinc';

comment
on column INSTRUMENTS_TESTS.generic_test_name is 'NOVA internal test name';

comment
on column INSTRUMENTS_TESTS.valid_sar_test is '''T''=have sex-age-range test,''F''=no sex-age-range test';

comment
on column INSTRUMENTS_TESTS.instruments_tests_num is 'uuid of this table';

comment
on column INSTRUMENTS_TESTS.test_transmit_name is 'Test name transmitted to external device (i.e. LIS, etc)';

comment
on column INSTRUMENTS_TESTS.send_to_inst is '''T'' if this record can be sent to instrument, ''F'' or NULL if not';

comment
on column INSTRUMENTS_TESTS.test_code_system is 'Source for CODE_SYSTEM value in the DML RANGE section.Code system of code of test name (i.e. LOINC,NOVABIO,etc).';

create table LOTS
(
    lots_key_num   char(36) not null
        constraint ASA82
            primary key,
    lot            char(80),
    expDate        date(23),
    lot_type       char(32),
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    Remaining      char(20),
    usedCount      int(10),
    lot_name       char(144),
    in_use         char(1),
    retired        char(1),
    use_before     date(23),
    is_validated   char(1)
);

comment
on column LOTS.lots_key_num is 'UID of this lot or record';

comment
on column LOTS.lot is 'The lot number';

comment
on column LOTS.lot_type is '"Control" or "Reagent" or "Linearity" or "TestStrip"';

comment
on column LOTS.in_use is '''T'' if the lot is in use, ''F'' if not. This column refers to whether the lot is temporarily disabled from use ("in_use" = "F"), but not necessarily retired.';

comment
on column LOTS.retired is '''T'' if the lot is retired (not use any more), ''F'' if not';

comment
on column LOTS.is_validated is '''T'' if UI has already seen this lot, ''F'' if not yet';

create table OPERATORS
(
    operator_num     char(36) not null
        constraint ASA92
            primary key,
    supervisor_num   char(36),
    operator_id      char(144),
    is_supervisor    char(1),
    arch             char(1),
    last_update_date timestamp(23)
        constraint LAST_UPDATE_DATE
            unique,
    datetime_stamp   timestamp(23) default timestamp,
    locked_by        char(320),
    add_date         timestamp(23)
);

comment
on column OPERATORS.operator_num is 'UID for this operator';

comment
on column OPERATORS.supervisor_num is 'UID for the operator that is this operator''s supervisor';

comment
on column OPERATORS.operator_id is 'The operator ID -- this is a unique ID usually received from Telcor';

comment
on column OPERATORS.is_supervisor is '''T'' if this operator is a supervisor,''F'' if not.(Not use any more after revision 1.72)';

comment
on column OPERATORS.arch is '''T'' during the time this operator is in the process of being archived,''F'' if not';

comment
on column OPERATORS.last_update_date is 'Date/Time any column in this record was last changed';

comment
on column OPERATORS.add_date is 'Date record added';

create table ORDERS
(
    order_Date       timestamp(23)
        constraint ORDER_DATE_TIME
            unique,
    accession_num    char(30),
    order_key_num    char(36) not null
        constraint ASA153
            primary key,
    transmitted_flag char(1),
    patient_id       char(30),
    medrec_num       char(30),
    account_num      char(30),
    loc_name         char(30),
    fac_name         char(30),
    panel            char(30),
    weight           char(6),
    weight_units     char(6),
    height           char(6),
    height_units     char(6),
    race             char(10),
    diagnosis        char(100),
    sample_type      char(20),
    instrument_id    char(20)
);

create table PATIENTS
(
    Patient_ID         char(84) not null,
    Last_Name          char(320),
    First_Name         char(320),
    Middle_Name        char(320),
    Sex                char(1),
    birthdate          date(23),
    last_activity_Date timestamp(23) default timestamp constraint LAST_PAT_ACT
			unique,
    arch               char(1),
    prefix             char(160),
    suffix             char(160),
    patient_uuid       char(36) not null
        constraint ASA146
            primary key,
    medrec_num         char(84) not null,
    add_date           timestamp(23),
    last_update_date   timestamp(23),
    ui_created_record  char(16),
    facil_num          char(36) not null,
    race               char(30),
    constraint PatientIDs
        unique (Patient_ID, medrec_num, facil_num)
);

comment
on column PATIENTS.Patient_ID is 'enterprise ID';

create table PATIENT_ACCOUNTS
(
    last_activity_Date timestamp(23) default timestamp constraint LAST_PAT_ACT
			unique,
    arch               char(1),
    patient_uuid       char(36) not null
        constraint PATIENTS
            references PATIENTS,
    account_num        char(84) not null,
    add_date           timestamp(23),
    ui_created_record  char(3),
    account_uuid       char(36) not null
        constraint ASA150
            primary key,
    constraint AccountNum
        unique (patient_uuid, account_num)
);

create table PATIENT_INCREMENTAL_D
(
    patient_uuid     char(36)      not null,
    loc_num          char(36),
    last_update_dttm timestamp(23) not null,
    patient_id       char(36),
    medrec_num       char(36),
    account_num      char(84),
    facil_num        char(36),
    row_num          char(36)      not null
        constraint ASA152
            primary key
);

create table PATIENT_VISITS
(
    Notes                      char(256),
    last_activity_Date         timestamp(23) default timestamp constraint LAST_PAT_ACT
			unique,
    arch                       char(1),
    discharge_time             timestamp(23),
    patient_uuid               char(36) not null
        constraint PATIENTS
            references PATIENTS,
    account_uuid               char(36) not null
        constraint PATIENT_ACCOUNTS
            references PATIENT_ACCOUNTS,
    Attend_Physician           char(144),
    Report_Physician           char(144),
    Consult_Physician          char(144),
    add_date                   timestamp(23),
    ui_created_record          char(3),
    visit_num                  char(84) not null,
    admit_time                 timestamp(23),
    patient_class              char(24),
    patient_type               char(24),
    visit_uuid                 char(36) not null
        constraint ASA148
            primary key,
    loc_num                    char(36) not null,
    room_num                   char(10),
    bed_num                    char(8),
    weight                     char(6),
    weight_units               char(6),
    height                     char(6),
    height_units               char(6),
    diagnosis                  char(100),
    d_list_flag                char(1),
    Deprecated_For_Account_num char(1),
    Deprecated_For_Medrec_Num  char(1),
    Deprecated_For_Patient_ID  char(1),
    last_update_date           timestamp(23),
    constraint VisitNums
        unique (account_uuid, visit_num)
);

create table PHYSICIANS
(
    Physician_ID     char(144) not null
        constraint ASA97
            primary key,
    Last_Name        char(320),
    First_Name       char(320),
    Middle_Name      char(320),
    prefix           char(160),
    suffix           char(160),
    datetime_stamp   timestamp(23) default timestamp,
    locked_by        char(320),
    add_date         timestamp(23),
    last_update_date timestamp(23),
    delete_date      timestamp(23)
);

create table SAMPLES
(
    sample_key_num           char(36) not null
        constraint ASA78
            primary key,
    Accession_num            char(84),
    sample_Date              timestamp(23)
        constraint SAMPLE_DATE
            unique,
    transmitted_flag         char(1),
    datetime_stamp           timestamp(23) default timestamp,
    control_type             char(32),
    control_lot_num          char(80),
    strip_lot_num            char(80),
    xml_text                 long varchar(max),
    patient_id               char(82),
    medrec_num               char(82),
    account_num              char(82),
    fac_name                 char(32),
    loc_name                 char(32),
    device_serial            char(32),
    saved_to_history_db_flag char(1)       default 'F',
    device_type              char(20),
    device_sw_ver            char(30),
    device_name              char(160),
    lot_level                char(2),
    internal_external        char(8)
);

comment
on column SAMPLES.control_type is '"UNK"=unknown, "OBS"=observation (patient result), "PRF"=proficiency,"CVR"=calibration verification (linearity), "EQC"=electronic QC or "LQC"=liquid QC';

comment
on column SAMPLES.control_lot_num is 'The lot number of the QC or Linearity sample (if the Control_Type is "CVR" or "LQC")';

comment
on column SAMPLES.strip_lot_num is 'The lot number of the strip that was used with the sample';

comment
on column SAMPLES.xml_text is 'Content of the result message';

create table SAMPLE_TYPES
(
    sample_type_code          char(12) not null,
    sample_type_name          char(80) not null
        constraint ASA104
            primary key,
    datetime_stamp            timestamp(23) default timestamp,
    locked_by                 char(320),
    sample_type_transmit_name char(80)
);

comment
on column SAMPLE_TYPES.sample_type_code is 'LOINC code for sample type';

comment
on column SAMPLE_TYPES.sample_type_name is 'Sample type from device';

comment
on column SAMPLE_TYPES.sample_type_transmit_name is 'Sample type transmitted to external device (i.e. LIS, etc)';

create table Test_Offsets
(
    generic_test_name char(30) not null,
    units             char(80) not null,
    slope             char(30),
    intercept         char(30),
    inst_type         char(20) not null,
    loc_num           char(36) not null,
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    enable_deselect   char(1),
    constraint ASA103
        primary key (generic_test_name, units, inst_type, loc_num)
);

comment
on column Test_Offsets.generic_test_name is 'NOVA internal test name';

comment
on column Test_Offsets.inst_type is 'The type of instrument to which the offset values apply';

comment
on column Test_Offsets.loc_num is 'The location number of the unit to which the offset values apply';

comment
on column Test_Offsets.enable_deselect is '''T'' if the ability of de-select GFR for Creatinine meter is applied, ''F'' if not. Required by ICPMGR.';

create table arch_export_backup
(
    data_type         char(256) not null,
    arch_export       char(1)   not null,
    is_select         char(1),
    older_than_months smallint(5),
    do_datetime       timestamp(23),
    interval          smallint(5),
    do_now            char(1),
    file_path         char(1024),
    loc_num           char(36),
    from_date         timestamp(23),
    to_date           timestamp(23),
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    constraint ASA86
        primary key (data_type, arch_export)
);

comment
on column arch_export_backup.data_type is 'Full name of archive/export data type or "Database Backup"';

comment
on column arch_export_backup.arch_export is '''T''= Archive,''F''= Export;''T''= Backup if database backup record';

comment
on column arch_export_backup.is_select is '''T''= Select this data type for Archive/Export or Setup daily backup';

comment
on column arch_export_backup.older_than_months is 'The data older than this setup will be archived';

comment
on column arch_export_backup.do_datetime is 'Date and time for archive or database backup';

comment
on column arch_export_backup.interval is 'Interval months from last time archiving or database backup';

comment
on column arch_export_backup.do_now is '''T''= Archive data or Backup database right now';

comment
on column arch_export_backup.file_path is 'Archive/Export or Backup file path';

comment
on column arch_export_backup.loc_num is 'The location Archive/Export data are for';

comment
on column arch_export_backup.from_date is 'The start date of selected archive/export data';

comment
on column arch_export_backup.to_date is 'The end date of selected archive/export data';

create table authorized_services
(
    service_name  char(64)  not null,
    pop_name      char(128) not null,
    enabled       char(1)   not null,
    runtime_error char(20),
    error_descr   char(128),
    constraint ASA141
        primary key (service_name, pop_name)
);

comment
on column authorized_services.service_name is 'name of the service of executable';

comment
on column authorized_services.pop_name is 'name of the point of presence server';

comment
on column authorized_services.enabled is 'T or F';

comment
on column authorized_services.runtime_error is 'code for why service cannot start';

comment
on column authorized_services.error_descr is 'description for why service cannot start';

create table component_update
(
    cu_uuid              char(36)  not null
        constraint ASA157
            primary key,
    cu_exec_path         char(512) not null,
    cu_install_is_active char(1)   not null,
    cu_datetime          timestamp(23) default current timestamp not null,
    cu_title             char(64)  not null,
    cu_description       char(512) not null
);

create table config_data
(
    config_num     char(36)  not null,
    directive_name char(32)  not null,
    "_key"         char(256) not null,
    "_value"       char(256),
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    constraint ASA96
        primary key (config_num, directive_name, "_key"),
    constraint ixc_DBA_index_consultant0_1
        unique ("_key", config_num)
);

create table contact_info
(
    contact_num    char(36) not null
        constraint ASA94
            primary key,
    last_name      char(320),
    first_name     char(320),
    initials       char(80),
    title          char(128),
    phone1         char(128),
    phone2         char(128),
    phone3         char(128),
    phone4         char(128),
    phone1_desc    char(256),
    phone2_desc    char(256),
    phone3_desc    char(256),
    phone4_desc    char(256),
    addr1          char(256),
    addr2          char(256),
    addr3          char(256),
    addr4          char(256),
    email          char(256),
    instant_msg    char(256),
    note           char(8192),
    ref_table      char(32),
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320)
);

comment
on column contact_info.contact_num is 'UID of contact info record';

comment
on column contact_info.ref_table is 'The name of table which the record is referenced by';

create table device_to_lot
(
    lots_key_num char(32) not null,
    inst_type    char(20) not null,
    constraint ASA123
        primary key (lots_key_num, inst_type)
);

comment
on table device_to_lot is 'This table will allow us to create and manage lot entries that will support multiple devices.';

comment
on column device_to_lot.lots_key_num is 'The uuid of the lot that the named device supports';

comment
on column device_to_lot.inst_type is 'The name of the device that supports the referenced lot';

create table diagnosis_codes
(
    diagnosis_code    char(16)  not null,
    diagnosis_text    char(512) not null,
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    generic_test_name char(30)  not null,
    constraint ASA99
        primary key (diagnosis_code, generic_test_name)
);

comment
on column diagnosis_codes.generic_test_name is 'NOVA internal test name';

create table discon_by_loc_profiles
(
    loc_num char(32) not null
        constraint ASA130
            primary key,
    active  char(1)  not null,
    minutes char(2)  not null
);

create table facility_patient_test_rails
(
    facility_id char(36) not null,
    test_name   char(12) not null,
    units       char(8)  not null,
    lo_limit    char(8),
    hi_limit    char(8),
    constraint ASA133
        primary key (facility_id, test_name, units)
);

create table facility_prefs
(
    facility_uuid                     varchar(36)           not null
        constraint ASA115
            primary key,
    PatIdMinLength                    varchar(2)            not null,
    PatIdMaxLength                    varchar(2)            not null,
    PatId1DMask                       varchar(24)           not null,
    PatId1DMaskLong                   varchar(24)           not null,
    PatId2DMask                       varchar(60)           not null,
    MrnMinLength                      varchar(2)            not null,
    MrnMaxLength                      varchar(2)            not null,
    Mrn1DMask                         varchar(24)           not null,
    Mrn1DMaskLong                     varchar(24)           not null,
    Mrn2DMask                         varchar(60)           not null,
    AcctNumMinLength                  varchar(2)            not null,
    AcctNumMaxLength                  varchar(2)            not null,
    AcctNum1DMask                     varchar(24)           not null,
    AcctNum1DMaskLong                 varchar(24)           not null,
    AcctNum2DMask                     varchar(60)           not null,
    AccnIdMinLength                   varchar(2)            not null,
    AccnIdMaxLength                   varchar(2)            not null,
    AccnId1DMask                      varchar(24)           not null,
    AccnId1DMaskLong                  varchar(24)           not null,
    AccnId2DMask                      varchar(60)           not null,
    DxIdMinLength                     char(2)  default '--' not null,
    DxIdMaxLength                     char(2)  default '--' not null,
    DxId1DMask                        char(24) default '--' not null,
    DxId1DMaskLong                    char(24) default '--' not null,
    DxId2DMask                        char(60) default '--' not null,
    OpLoginMinLength                  char(2)  default '--' not null,
    OpLoginMaxLength                  char(2)  default '--' not null,
    OpLogin1DMask                     char(24) default '--' not null,
    OpLogin1DMaskLong                 char(24) default '--' not null,
    OpLogin2DMask                     char(60) default '--' not null,
    PhysIdMinLength                   char(2)  default '--' not null,
    PhysIdMaxLength                   char(2)  default '--' not null,
    PhysId1DMask                      char(24) default '--' not null,
    PhysId1DMaskLong                  char(24) default '--' not null,
    PhysId2DMask                      char(60) default '--' not null,
    lis_feed_state_has_been_validated char(1)  default 'F',
    time_zone                         char(32),
    date_format                       char(5),
    lis_feed_active                   char(1),
    time_format                       char(8)
);

create table facility_setup_prefs
(
    creatinine_default_uom char(12) not null,
    ketone_default_uom     char(12) not null,
    glucose_default_uom    char(12) not null,
    lactate_default_uom    char(12) not null,
    user_allowed_to_alter  char(1)  not null,
    weight_units           char(3)  not null,
    height_units           char(3)  not null
);

create table health_ping
(
    process_name           char(20)                 not null,
    host                   char(200)                not null,
    update_time            timestamp(23) default '' not null,
    do_log                 char(1),
    last_start_dttm        timestamp(23),
    log_expire_days        int(10),
    last_connect_dttm      timestamp(23),
    last_disconnect_dttm   timestamp(23),
    num_messages_processed int(10),
    tot_messages_processed int(10),
    messages_at_last_cycle int(10),
    show_services          char(1)       default 'F',
    constraint ASA111
        primary key (process_name, host),
    constraint ixc_DM2_4
        unique (host, process_name)
);

comment
on table health_ping is 'This is a table that will be filled in with ping reports from non-soa complaint components, such as MGR and ICP.';

comment
on column health_ping.process_name is 'Component name, such as ICP or MGR';

comment
on column health_ping.host is 'Host name where the component resides';

comment
on column health_ping.update_time is 'Last time this row updated';

comment
on column health_ping.do_log is '''T'' if done logging in for the host, ''F'' if not. Required by ICP.';

create table inst_locations
(
    loc_num                   char(36)                  not null
        constraint ASA90
            primary key,
    parent                    char(36)      default '0' not null,
    level_num                 smallint(5) not null,
    loc_name                  char(128)                 not null,
    last_pat_update           timestamp(23),
    last_op_update            timestamp(23),
    datetime_stamp            timestamp(23) default timestamp,
    locked_by                 char(320),
    is_default                char(1)       default 'F',
    restrict_to_local_queries char(1),
    constraint "inst_locations UNIQUE (parent,level_num,loc_name)"
        unique (parent, level_num, loc_name),
    constraint ixc_DM2_5
        unique (loc_name, level_num),
    constraint ixc_DM2_6
        unique (loc_name, parent)
);

comment
on column inst_locations.last_pat_update is 'The date/time of the last patient list update for the location';

comment
on column inst_locations.last_op_update is 'The date/time of the last operator list update for the location';

create table LOT_CHEM
(
    lots_key_num      char(36) not null
        constraint LOTS
            references LOTS,
    generic_test_name char(30) not null,
    lot_level         char(80),
    level_type        char(32),
    LR                char(16),
    HR                char(16),
    TM                real(7),
    TSD               real(7),
    Units             char(80) not null,
    side_mean         smallint(5),
    c2_2s             smallint(5),
    c4_1s             smallint(5),
    c10x              smallint(5),
    range_method      smallint(5),
    use_WR1           char(1),
    use_WR2           char(1),
    use_WR3           char(1),
    use_WR4           char(1),
    use_WR5           char(1),
    chem_value        char(120),
    display_order     int(10),
    dps               int(10),
    order_calc_flag   char(1),
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    facility_num      char(36) not null
        constraint inst_locations
            references inst_locations,
    constraint ASA81
        primary key (lots_key_num, generic_test_name, Units, facility_num)
);

comment
on column LOT_CHEM.lots_key_num is 'UID of the lot key number';

comment
on column LOT_CHEM.generic_test_name is 'NOVA internal test name';

comment
on column LOT_CHEM.lot_level is 'The level number associated with the lot (lots_key_num)';

comment
on column LOT_CHEM.level_type is '"Control"(QC) 1 - "Control" 3 and "Linearity" 1 - "Linearity" 5 if TestStrip Lot;"Control" 1 - "Control" n if QC/Control Lot;"Reagent" 1 if Reagent Lot (a Reagent Lot may has only one level).';

comment
on column LOT_CHEM.LR is 'Low range limit';

comment
on column LOT_CHEM.HR is 'High range limit';

comment
on column LOT_CHEM.TM is 'Targets mean';

comment
on column LOT_CHEM.TSD is 'Targets standard deviation';

comment
on column LOT_CHEM.Units is 'The chemistry units';

comment
on column LOT_CHEM.side_mean is '''+'' if mean''s side beyond, ''-'' if mean''s side below';

comment
on column LOT_CHEM.c2_2s is 'The count of westgard 2-2s that is two consecutive samples greater than two SDs from the mean.';

comment
on column LOT_CHEM.c4_1s is 'The count of westgard 4-1s that is four consecutive samples greater than one SD from the mean.';

comment
on column LOT_CHEM.c10x is 'The count of westgard 10x that is ten consecutive samples on the same side of the mean.';

comment
on column LOT_CHEM.range_method is '0 if entered range method, 1 if % of Mean range method, 2 if number of standard deviations range method, 3 if fixed range method.';

comment
on column LOT_CHEM.use_WR1 is '''T'' if using #1 of westgard rules, ''F'' if not';

comment
on column LOT_CHEM.use_WR2 is '''T'' if using #2 of westgard rules, ''F'' if not';

comment
on column LOT_CHEM.use_WR3 is '''T'' if using #3 of westgard rules, ''F'' if not';

comment
on column LOT_CHEM.use_WR4 is '''T'' if using #4 of westgard rules, ''F'' if not';

comment
on column LOT_CHEM.use_WR5 is '''T'' if using #5 of westgard rules, ''F'' if not';

create table auto_discharge_fac
(
    uuid          char(36) not null
        constraint "auto_discharge_fac UNIQUE (uuid)"
            unique,
    loc_num       char(36) not null
        constraint inst_locations
            references inst_locations,
    class_type    char(1)  not null,
    adt_id        char(12) not null,
    retain_hours  char(4)  not null,
    ui_hours_days char(1)  not null,
    constraint ASA137
        primary key (loc_num, class_type, adt_id)
);

create table auto_discharge_loc
(
    parent_uuid   char(36) not null,
    loc_num       char(36) not null
        constraint inst_locations
            references inst_locations,
    adt_id        char(12) not null,
    retain_hours  char(4)  not null,
    ui_hours_days char(1)  not null,
    constraint ASA139
        primary key (parent_uuid, loc_num, adt_id)
);

create table bga_setup_to_location
(
    loc_num            varchar(36)                     not null
        constraint inst_locations
            references inst_locations,
    update_dttm        timestamp(23) default timestamp,
    busy               char(1),
    loc_setup_ref_uuid char(32)      default 'newid()' not null,
    inst_type          char(20)                        not null
        constraint INSTRUMENT_TYPES
            references INSTRUMENT_TYPES,
    inst_num           char(36),
    constraint ASA161
        primary key (inst_type, loc_num)
);

create table diagnosis_to_unit
(
    diagnosis_code    char(16) not null,
    loc_num           char(36) not null
        constraint inst_locations
            references inst_locations,
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    generic_test_name char(30) not null,
    constraint ASA100
        primary key (diagnosis_code, loc_num, generic_test_name)
);

create table facility_ht_wt_units
(
    loc_num      char(36) not null
        constraint ASA113
            primary key
        constraint inst_locations
            references inst_locations,
    weight_units char(20),
    height_units char(20)
);

comment
on table facility_ht_wt_units is 'This is where the height and weight units system will be kept for applyiny to facilities';

comment
on column facility_ht_wt_units.loc_num is 'Facility ID';

comment
on column facility_ht_wt_units.weight_units is 'Weight units system. LBS=pounds, KGS=kilograms';

comment
on column facility_ht_wt_units.height_units is 'Height units system. INS=inches, CMS=centimeters';

create table facility_test_units
(
    generic_test_name char(30) not null,
    units_of_measure  char(80) not null,
    loc_num           char(36) not null
        constraint inst_locations
            references inst_locations,
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    constraint ASA109
        primary key (generic_test_name, units_of_measure, loc_num)
);

create table instruments
(
    inst_num             char(36)  not null
        constraint ASA83
            primary key,
    inst_type            char(20)
        constraint instrument_types
            references INSTRUMENT_TYPES,
    inst_name            char(160),
    inst_id              char(144) not null
        constraint "instruments UNIQUE (inst_id)"
            unique,
    serial_no            char(80),
    loc_num              char(36),
    port_num             int(10),
    tran_id              char(144),
    inst_printer         char(320),
    inst_active          int(10),
    sw_version           char(30),
    last_pat_dload       timestamp(23),
    last_op_dload        timestamp(23),
    datetime_stamp       timestamp(23) default timestamp,
    locked_by            char(320),
    inst_condition       char(20),
    last_connect_dttm    timestamp(23),
    last_disconnect_dttm timestamp(23),
    ip_address           char(19),
    computer_name        char(120),
    dock_lock_time       timestamp(23),
    total_patients       int(10),
    total_operators      int(10),
    wifi_mac_address     char(20),
    mac_address          char(20)
);

comment
on table instruments is 'Instrument''s MAC address';

comment
on column instruments.last_pat_dload is 'The date/time of the last patient list download for the instrument';

comment
on column instruments.last_op_dload is 'The date/time of the last operator list download for the instrument';

comment
on column instruments.inst_condition is 'The condition status of the instrument.''L''=QC Lockout,''P''=Partial QC Lockout,''R''=Ready,''B''=Busy,''S''=Standby.';

comment
on column instruments.wifi_mac_address is 'Instrument''s Wi-Fi MAC address';

comment
on column instruments.mac_address is 'Instrument''s MAC address';

create table bga_cartridge_status
(
    cartridge_type   varchar(30) not null,
    inst_num         varchar(36) not null
        constraint instruments
            references instruments,
    lot_num          varchar(20) not null,
    remaining_volume varchar(10),
    status           varchar(1024),
    update_dttm      timestamp(23) default timestamp,
    constraint ASA160
        primary key (cartridge_type, inst_num)
);

create table bga_state
(
    inst_num      varchar(36) not null
        constraint ASA159
            primary key
        constraint instruments
            references instruments,
    state_major   varchar(30) not null,
    state_details varchar(50),
    update_dttm   timestamp(23) default timestamp
);

create table bga_test_status
(
    inst_num       varchar(36) not null
        constraint instruments
            references instruments,
    is_calibrated  char(1),
    is_qc_lockout  char(1),
    observation_id varchar(20) not null,
    test_issues    varchar(1024),
    update_dttm    timestamp(23) default timestamp,
    constraint ASA158
        primary key (inst_num, observation_id)
);

create table device_events
(
    event_type     char(1),
    date_done      timestamp(23),
    inst_num       char(36)
        constraint instruments
            references instruments,
    operator_num   char(36)
        constraint OPERATORS
            references OPERATORS,
    arch           char(1),
    event_desc     char(1000),
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    uuid           char(32) not null
        constraint ASA85
            primary key
);

comment
on column device_events.event_type is '''M''=maintenance event,''E''=error event,''O''=others';

create table loc_def_pat_id
(
    loc_num    char(36) not null
        constraint ASA110
            primary key
        constraint inst_locations
            references inst_locations,
    def_pat_id char(10) not null
);

comment
on table loc_def_pat_id is 'This is where we keep the default required patient id for each location';

comment
on column loc_def_pat_id.loc_num is 'The location id';

comment
on column loc_def_pat_id.def_pat_id is 'This can be PATID, MRN or ACCT - MRN is the default';

create table loc_last_update
(
    loc_num          char(36) not null,
    data_type        char(36) not null,
    inst_type        char(20) not null,
    last_update_time timestamp(23) default timestamp not null,
    constraint ASA114
        primary key (loc_num, data_type, inst_type),
    constraint ixc_DM2_1
        unique (loc_num, inst_type, data_type, last_update_time)
);

comment
on table loc_last_update is 'This is where the gui stores last update time for download lists with locations';

create table loc_to_config
(
    loc_num        char(36) not null
        constraint inst_locations
            references inst_locations,
    inst_type      char(20) not null
        constraint INSTRUMENT_TYPES
            references INSTRUMENT_TYPES,
    config_num     char(36) not null,
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    config_name    char(128),
    is_global      char(1)       default 'F',
    constraint ASA95
        primary key (loc_num, inst_type, config_num),
    constraint ixc_DBA_index_consultant0_2
        unique (loc_num, inst_type),
    constraint ixc_DM2_2
        unique (config_num, loc_num)
);

comment
on column loc_to_config.is_global is '''T''=global,''F''=not global';

create table loc_to_firmware
(
    loc_num        char(36) not null,
    firmware_num   char(36) not null,
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    inst_type      char(20) not null,
    constraint ASA108
        primary key (loc_num, firmware_num)
);

comment
on table loc_to_firmware is 'Intersect between the inst_locations and firmware tables';

comment
on column loc_to_firmware.loc_num is 'UUID of the referenced inst_locations row';

comment
on column loc_to_firmware.firmware_num is 'UUID of the referenced firmware row';

create table loc_to_panel
(
    loc_num    char(32) not null,
    panel_name char(32) not null
);

create table loc_to_wifi_setup
(
    config_id  char(36) not null,
    loc_num    char(36) not null,
    inst_class char(36) not null
);

create table location_alias
(
    loc_id char(36)  not null,
    alias  char(128) not null,
    constraint ASA140
        primary key (loc_id, alias)
);

create table lot_chem_defaults
(
    generic_test_name char(20) not null,
    lot_level         char(10) not null,
    lot_type          char(10) not null,
    units_of_measure  char(10) not null,
    lo_default        char(10) not null,
    hi_default        char(10) not null,
    lot_num_id        char(10) not null,
    constraint ASA124
        primary key (generic_test_name, lot_level, lot_type, units_of_measure, lot_num_id)
);

comment
on table lot_chem_defaults is 'This is where we keep the default low and high range defaults for a lot.
We use the low and high values from this table when we';

comment
on column lot_chem_defaults.generic_test_name is 'Glucose, Creatinine,Ketone...etc';

comment
on column lot_chem_defaults.lot_level is 'The level number';

comment
on column lot_chem_defaults.lot_type is '''Control'' or ''Linearity''';

comment
on column lot_chem_defaults.units_of_measure is 'Units of Measure';

comment
on column lot_chem_defaults.lo_default is 'Low default';

comment
on column lot_chem_defaults.hi_default is 'High Default';

comment
on column lot_chem_defaults.lot_num_id is 'First two chars of the lot number';

create table lot_insert_ranges
(
    lots_key_num      char(36) not null,
    lo_value          char(10) not null,
    hi_value          char(10) not null,
    units_of_measure  char(10) not null,
    generic_test_name char(20) not null,
    constraint ASA127
        primary key (lots_key_num, units_of_measure, generic_test_name)
);

create table lots_to_unit
(
    lots_key_num   char(36) not null
        constraint LOTS
            references LOTS,
    loc_num        char(36) not null
        constraint inst_locations
            references inst_locations,
    datetime_stamp timestamp(23) default timestamp,
    locked_by      char(320),
    fac_num        char(36),
    constraint ASA102
        primary key (lots_key_num, loc_num)
);

comment
on column lots_to_unit.loc_num is 'unit num';

create table maint_plan
(
    plan_id                         unsigned int(10) default autoincrement not null
        constraint maint_plan_pk
        primary key,
    plan_name                       varchar(128) not null
        constraint maint_plan_uc
            unique,
    event_name                      varchar(128),
    disable_new_connections         bit(1)       not null,
    disconnect_all_users            bit(1)       not null,
    do_validate                     bit(1)       not null,
    validate_database_check         bit(1)       not null,
    validate_checksum_check         bit(1)       not null,
    validate_express_check          bit(1)       not null,
    validate_normal_check           bit(1)       not null,
    do_backup                       bit(1)       not null,
    disk_backup                     bit(1)       not null,
    full_backup                     bit(1)       not null,
    archive_backup                  bit(1)       not null,
    backup_path                     long varchar(max),
    tape_backup_prompt              bit(1)       not null,
    tape_backup_comment             long varchar(max),
    save_report_count               int(10),
    report_to_console               bit(1)       not null,
    email_success                   bit(1)       not null,
    email_failure                   bit(1)       not null,
    email_recipients                long varchar(max),
    email_smtp_server_name          long varchar(max),
    email_smtp_port                 int(10),
    email_smtp_sender_name          long varchar(max),
    email_smtp_sender_address       long varchar(max),
    email_smtp_auth_user_name       long varchar(max),
    email_smtp_auth_password        long varchar(max),
    email_user_id                   long varchar(max),
    email_user_password             long varchar(max),
    email_smtp_trusted_certificates long varchar(max),
    email_smtp_certificate_company  long varchar(max),
    email_smtp_certificate_unit     long varchar(max),
    email_smtp_certificate_name     long varchar(max),
    custom_prevalidation_sql        long varchar(max),
    custom_postbackup_sql           long varchar(max)
);

create table maint_plan_report
(
    plan_id     unsigned int(10) not null
        constraint maint_plan_report_fk
        references maint_plan
        on delete cascade,
    start_time  timestamp(23) not null,
    finish_time timestamp(23),
    success     bit(1)        not null,
    report      long varchar(max),
    constraint maint_plan_report_pk
        primary key (plan_id, start_time)
);

create table manual_tests
(
    mt_num         char(36) not null
        constraint ASA128
            primary key,
    panel_name     char(36) not null,
    test_type      char(10) not null,
    xml_data       xml(max) not null,
    datetime_stamp timestamp(23) default timestamp,
    test_name      char(36) not null
        constraint "manual_tests UNIQUE (test_name)"
            unique
);

create table mt_lot_to_loc
(
    loc_num      char(32) not null,
    lots_key_num char(32) not null
);

create table operator_message
(
    operator_num            char(36)      not null
        constraint OPERATORS
            references OPERATORS,
    opr_message             char(200)     not null,
    msg_create_dttm         timestamp(23) not null,
    msg_read_dttm           timestamp(23),
    msg_priority            char(1),
    datetime_stamp          timestamp(23) default timestamp,
    locked_by               char(320),
    current_msg             char(1),
    auto_recert_warning_msg char(1),
    test                    char(32),
    fac_uuid                char(36),
    constraint ASA105
        primary key (operator_num, msg_create_dttm),
    constraint ixc_DM2_9
        unique (operator_num, current_msg)
);

comment
on column operator_message.operator_num is 'Operator ID, foreign key to operators table';

comment
on column operator_message.opr_message is 'Text of the message';

comment
on column operator_message.msg_create_dttm is 'Creation date/time of the message';

comment
on column operator_message.msg_read_dttm is 'Date/time the message was read';

comment
on column operator_message.msg_priority is '''T'' if this message takes priority over all others for the given operator, otherwise ''F''';

comment
on column operator_message.current_msg is '''T'' if this is the current msg for the specified operator, ''F'' if not';

comment
on column operator_message.fac_uuid is 'Facility origin of message';

create table operator_privilege
(
    operator_num               char(36) not null
        constraint OPERATORS
            references OPERATORS,
    inst_type                  char(20) not null
        constraint INSTRUMENT_TYPES
            references INSTRUMENT_TYPES,
    privilege                  int(10),
    pswd                       char(80),
    pswd_expire_date           date(23),
    last_update_date           timestamp(23),
    datetime_stamp             timestamp(23) default timestamp,
    locked_by                  char(320),
    cert_start_date            timestamp(23),
    cert_end_date              timestamp(23),
    is_active                  char(1),
    is_active_last_update_date timestamp(23)
        constraint IS_ACT_LAST_UPDATE_DATE
            unique,
    test_name                  char(20) not null,
    constraint ASA93
        primary key (operator_num, inst_type, test_name)
);

comment
on column operator_privilege.operator_num is 'UID for the operator whose privileges are defined here';

comment
on column operator_privilege.inst_type is 'The instrument type to which the privileges defined here apply';

comment
on column operator_privilege.privilege is 'The privilege accorded to the referenced operator on the specified instrument. 1=Supervisor,4=User,5=Service,6=Training on device access';

comment
on column operator_privilege.pswd is 'The referenced operators password on the specified instrument';

comment
on column operator_privilege.pswd_expire_date is 'The last date during which the specified password is valid';

comment
on column operator_privilege.last_update_date is 'The date this privilege was last updated';

comment
on column operator_privilege.cert_start_date is 'The first date for which the referenced operator is certified to perform the test/panel associated with instrument type';

comment
on column operator_privilege.cert_end_date is 'The last date for which the referenced operator is certified to perform the test/panel associated with instrument type';

comment
on column operator_privilege.is_active is '''T'' if this operator is active, ''F'' if not';

comment
on column operator_privilege.is_active_last_update_date is 'Date/Time the is_active column was last changed';

comment
on column operator_privilege.test_name is 'Generic Test Name';

create table operator_to_unit
(
    operator_num               char(36) not null
        constraint OPERATORS
            references OPERATORS,
    loc_num                    char(36) not null
        constraint inst_locations
            references inst_locations,
    is_default                 char(1),
    is_active                  char(1),
    is_active_last_update_date timestamp(23)
        constraint IS_ACT_LAST_UPDATE_DATE
            unique,
    datetime_stamp             timestamp(23) default timestamp,
    locked_by                  char(320),
    constraint ASA106
        primary key (operator_num, loc_num)
);

comment
on column operator_to_unit.operator_num is 'UID of the operator';

comment
on column operator_to_unit.loc_num is 'UID of the unit location';

comment
on column operator_to_unit.is_default is '''T'' if loc_num refers to the default unit for the referenced operator, ''F'' if not';

comment
on column operator_to_unit.is_active is '''T'' if this operator to unit assignment is active, ''F'' if not';

comment
on column operator_to_unit.is_active_last_update_date is 'The Date the is_active column was last updated';

create table physician_to_unit
(
    physician_id     char(144) not null,
    loc_num          char(36)  not null
        constraint inst_locations
            references inst_locations,
    datetime_stamp   timestamp(23) default timestamp,
    locked_by        char(320),
    last_update_date timestamp(23),
    is_active        char(1)   not null,
    constraint ASA101
        primary key (physician_id, loc_num)
);

comment
on column physician_to_unit.loc_num is 'unit num';

comment
on column physician_to_unit.is_active is '''T''=add,''F''=delete';

create table pop_info
(
    pop_url    char(128) not null,
    ssl_state  char(1)   not null,
    time_stamp timestamp(23) default current timestamp not null
);

comment
on table pop_info is 'Here we keep any status/setup info we need for the named pop';

create table process_control
(
    pc_process char(64)  not null,
    pc_key     char(64)  not null,
    pc_value   char(255) not null,
    fac_uuid   char(36)  not null,
    loc_uuid   char(36)  not null,
    constraint ASA136
        primary key (pc_process, pc_key, loc_uuid, fac_uuid)
);

create table recertification_messages
(
    facility_id char(36) not null
        constraint ASA134
            primary key,
    enabled     char(1),
    the_message char(50),
    days_before char(2),
    frequency   char(5)
);

create table rta
(
    tn char(128)           not null
        constraint ASA135
            primary key,
    ts char(1) default 'F' not null
);

create table service_packs
(
    name             char(255) not null,
    service_datetime timestamp(23) default timestamp,
    desc             long varchar(max)
);

comment
on table service_packs is 'Service pack record as created by the service pack installer';

comment
on column service_packs.name is 'the service pack name';

comment
on column service_packs.service_datetime is 'date and time this record was created';

comment
on column service_packs.desc is 'description from the service pack installer';

create table test_comment
(
    comment_num       char(32) not null
        constraint ASA125
            primary key,
    comment_desc      char(32) not null,
    comment_type      char(20) not null,
    generic_test_name char(30) not null,
    display_order     smallint(5) not null,
    is_chartable      char(1)  not null,
    is_flagable       char(1)  not null
);

create table test_comment_to_loc
(
    comment_num char(32) not null,
    loc_num     char(32) not null,
    constraint ASA126
        primary key (comment_num, loc_num)
);

create table test_range
(
    generic_test_name char(30) not null,
    sample_type_code  char(12) not null,
    result_type_code  char(1)  not null,
    units             char(80) not null,
    lo_panic_limit    char(10) not null,
    hi_panic_limit    char(10) not null,
    lo_normal_limit   char(10) not null,
    hi_normal_limit   char(10) not null,
    sex               char(1)  not null,
    ageLo             char(3),
    ageHi             char(3),
    datetime_stamp    timestamp(23) default timestamp,
    locked_by         char(320),
    enable_all_ages   char(1),
    equation          char(50),
    eq_const          char(20),
    loc_num           char(36) not null,
    age_type          char(1)  not null,
    range_label       char(120),
    group_num         char(36) not null,
    ui_order          char(1)
);

comment
on column test_range.generic_test_name is 'NOVA internal test name';

comment
on column test_range.sample_type_code is 'LOINC code for sample type';

comment
on column test_range.result_type_code is 'LOINC code for result type. ''C''=Calculated,''D''=Default,''E''=Estimated,''I''=Input,''M''=Measured,''U''=Unknown';

comment
on column test_range.enable_all_ages is '''T''=enable,''F''=not enable';

comment
on column test_range.equation is 'Source for EQ value in the DML Range section. Equation method used in calculation of the calculated test:CG,MDRD,SZ and CB.';

comment
on column test_range.eq_const is 'Source for EQ_CONST value in the DML Range section. K constant for the age group in calculation of GFR.';

comment
on column test_range.loc_num is 'Location/units ID';

comment
on column test_range.age_type is '''D''=day or ''Y''=year';

comment
on column test_range.range_label is 'Label text for button';

comment
on column test_range.group_num is 'For grouping with other locations';

comment
on column test_range.ui_order is 'UI ordering within a generic test name';

create table time_zone
(
    zone_name   char(50) not null
        constraint ASA131
            primary key,
    zone_offset char(6)  not null
);

create table ui_comm_ports_page_control
(
    record_id             int(10) not null
        constraint ASA120
			primary key,
    device_name           char(20) not null,
    data_protocol         char(12) not null,
    comm_protocol         char(16) not null,
    multi_connect_capable char(1)  not null
);

create table ui_general
(
    base_lang     char(12)            not null,
    admin_lang    char(12)            not null,
    forward_login char(1) default '?' not null
);

create table ui_instruments
(
    name   char(40) not null
        constraint ASA116
            primary key,
    active char(3)  not null
);

comment
on column ui_instruments.active is '''yes'' or ''no''';

create table ui_menu_entry
(
    major          smallint(5) not null,
    minor          smallint(5) not null,
    source         char(255) not null,
    english_text   char(20)  not null,
    lock_holder    char(36)      default 'FREE',
    datetime_stamp timestamp(23) default current timestamp not null,
    constraint ASA117
        primary key (major, minor)
);

comment
on column ui_menu_entry.english_text is 'not used - just for info purposes';

create table ui_old_passwords
(
    password  char(16) not null,
    user_name char(16) not null
);

create table ui_password_properties
(
    require_one_char            char(1)             not null,
    require_one_numeric         char(1)             not null,
    require_one_special_char    char(1)             not null,
    enforce_length              char(1)             not null,
    minimum_length              char(2)             not null,
    maximum_length              char(2)             not null,
    disallow_old_passwords      char(1)             not null,
    password_lifetime_in_days   char(3)             not null,
    login_attempts_trip_count   char(1)             not null,
    enforce_login_attempts_lock char(1)             not null,
    hide_login_id               char(1) default 'F' not null
);

create table ui_sticky_attributes
(
    user_name    char(320) not null,
    attrib_page  char(30)  not null,
    attrib_name  char(30)  not null,
    attrib_value char(30)  not null,
    constraint ASA121
        primary key (user_name, attrib_page, attrib_name)
);

create table ui_user
(
    uid               char(36)  not null
        constraint ASA119
            primary key,
    user_name         char(320) not null,
    pass_word         char(80)  not null,
    first_name        char(320) not null,
    last_name         char(320) not null,
    acct_expire       char(96)  not null,
    session_time      char(2)   not null,
    online_state      char(40)  not null,
    phone_number      char(128),
    acct_never_expire char(1)   not null,
    pw_create_date    date(23),
    locked_out        char(1),
    invalid_attempts  char(1),
    constraint ixc_DM2_7
        unique (last_name, first_name)
);

create table ui_user_to_domain
(
    user_id   char(36) not null,
    domain_id char(6)  not null,
    constraint ASA118
        primary key (user_id, domain_id)
);

create table ui_user_to_facility
(
    user_uid     varchar(32) not null,
    facility_uid varchar(32) not null
);

create table user_alive
(
    user_name  char(36)                  not null
        constraint ASA164
            primary key,
    time_stamp timestamp(23) default current timestamp,
    nova_admin char(1)       default 'F' not null
);

create table version_info
(
    object_name char(36) not null
        constraint ASA132
            primary key,
    version     char(36)
);

create table wifi_certificate
(
    Datetime_stamp   timestamp(23) default current timestamp not null,
    certificate_name char(255) not null,
    certificate_num  char(36)  not null,
    inst_class       char(12)  not null,
    data_type        char(32)  not null,
    file_name        char(128) not null,
    constraint ASA143
        primary key (inst_class, data_type, certificate_name, file_name)
);

create table wifi_certificate_data_mine
(
    certificate_num  char(36)      not null,
    inst_class       char(12)      not null,
    index_num        int(10) not null,
    certificate_data varchar(1024) not null,
    data_type        char(32)      not null,
    constraint ASA144
        primary key (certificate_num, index_num)
);

create table wifi_certificate_to_device
(
    wifi_mac_address char(20)  not null,
    data_type        char(32)  not null,
    certificate_name char(255) not null,
    fac_num          char(36)  not null,
    loc_num          char(36)  not null,
    certificate_num  char(36)  not null,
    datetime_stamp   timestamp(23) default current timestamp,
    constraint ASA162
        primary key (wifi_mac_address, certificate_name, fac_num, loc_num, data_type)
);

comment
on table wifi_certificate_to_device is 'This is used to link a device specific SSL certificate(s) to the actual device';

create table wifi_certificate_to_location
(
    certificate_name char(255) not null,
    loc_num          char(36)  not null,
    inst_class       char(12)  not null,
    certificate_num  char(36)  not null,
    data_type        char(32)  not null,
    fac_num          char(36)  not null,
    constraint ASA145
        primary key (loc_num, inst_class, data_type, certificate_name, fac_num)
);

create table wifi_credentials
(
    wifi_mac_address char(20) not null,
    wifi_user_name   char(64) not null,
    wifi_password    char(64) not null,
    fac_num          char(36) not null,
    loc_num          char(36) not null,
    datetime_stamp   timestamp(23) default current timestamp,
    constraint ASA163
        primary key (wifi_mac_address, fac_num, loc_num)
);

comment
on table wifi_credentials is 'This is used to link a device specific User Name/Password to the actual device';

create table wifi_setup
(
    config_name char(64) not null,
    config_id   char(36) not null,
    fac_num     char(36) not null,
    inst_class  char(36) not null,
    wifi_data   long varchar(max) not null,
    constraint ASA142
        primary key (fac_num, inst_class, config_name)
);

