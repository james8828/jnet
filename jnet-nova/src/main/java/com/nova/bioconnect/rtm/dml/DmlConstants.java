package com.nova.bioconnect.rtm.dml;

public final class DmlConstants {
    private DmlConstants() {}

    public static final String MSG_HELLO = "HEL.R01";
    public static final String MSG_ACK_HELLO = "HEL.R02";
    public static final String MSG_SETUP = "SET.R01";
    public static final String MSG_ACK_SETUP = "SET.R02";
    public static final String MSG_DAILY_SETUP = "DST.R01";
    public static final String MSG_ACK_DAILY_SETUP = "DST.R02";
    public static final String MSG_OPL = "OPL.R01";
    public static final String MSG_ACK_OPL = "OPL.R02";
    public static final String MSG_PAT = "PAT.R01";
    public static final String MSG_ACK_PAT = "PAT.R02";
    public static final String MSG_OBS = "OBS.R01";
    public static final String MSG_ACK_OBS = "OBS.R02";
    public static final String MSG_SVC = "SVC.R01";
    public static final String MSG_ACK_SVC = "SVC.R02";
    public static final String MSG_CMD = "CMD.R01";
    public static final String MSG_ACK_CMD = "CMD.R02";
    public static final String MSG_TERMINATE = "TER.R01";
    public static final String MSG_ACK_TERMINATE = "TER.R02";

    public static final String ELEMENT_MESSAGE = "Message";
    public static final String ELEMENT_HEADER = "Header";
    public static final String ELEMENT_BODY = "Body";
    public static final String ELEMENT_TRAILER = "Trailer";
    public static final String ELEMENT_SVC = "SVC";
    public static final String ELEMENT_OBS = "OBS";
    public static final String ELEMENT_PAT = "PAT";
    public static final String ELEMENT_OPL = "OPL";
    public static final String ELEMENT_MPI = "MPI";

    public static final String ATTR_MESSAGE_TYPE = "Type";
    public static final String ATTR_VERSION = "Version";
    public static final String ATTR_MESSAGE_ID = "MessageId";
    public static final String ATTR_SESSION_ID = "SessionId";
    public static final String ATTR_ACK_CODE = "AckCode";
    public static final String ATTR_DEVICE_TYPE = "DeviceType";
    public static final String ATTR_SERIAL_NUMBER = "SerialNumber";
    public static final String ATTR_SW_VERSION = "SwVersion";
    public static final String ATTR_MODEL = "Model";
    public static final String ATTR_NAME = "Name";
    public static final String ATTR_VALUE = "Value";
    public static final String ATTR_CODE = "Code";
    public static final String ATTR_UNITS = "Units";
    public static final String ATTR_DATE = "Date";
    public static final String ATTR_TIME = "Time";
    public static final String ATTR_ID = "Id";
    public static final String ATTR_TYPE = "Type";
    public static final String ATTR_FIRST = "First";
    public static final String ATTR_LAST = "Last";
    public static final String ATTR_MIDDLE = "Middle";
    public static final String ATTR_SEX = "Sex";
    public static final String ATTR_BIRTHDATE = "Birthdate";
    public static final String ATTR_MRN = "MRN";
    public static final String ATTR_FACILITY = "Facility";
    public static final String ATTR_LOCATION = "Location";
    public static final String ATTR_DEPARTMENT = "Department";
    public static final String ATTR_OPERATOR_ID = "OperatorId";
    public static final String ATTR_OPERATOR_NAME = "OperatorName";
    public static final String ATTR_SUPERVISOR = "Supervisor";
    public static final String ATTR_PRIVILEGE = "Privilege";

    public static final String ACK_POSITIVE = "AA";
    public static final String ACK_NEGATIVE = "AE";

    public static final String DML_VERSION = "1.0";
}