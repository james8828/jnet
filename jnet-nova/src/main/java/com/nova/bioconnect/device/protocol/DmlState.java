package com.nova.bioconnect.device.protocol;

/**
 * DML Protocol State Machine States
 * Maps to C# DMLProtocol.DMLSTATE enum (case values in StepProtocolState)
 */
public enum DmlState {

    // 0, 2 - Exception states
    EXCEPTION(0),

    // 1 - Hello received, waiting for ACK
    HELLO_RECEIVED(1),

    // 3 - ACK Hello sent, waiting for DST
    ACK_HELLO(3),

    // 4 - Request observations from device
    REQ_OBS(4),

    // 5 - Observation EOT received
    OBS_EOT(5),

    // 6 - Request events from device
    REQ_EVS(6),

    // 7 - Event EOT received
    EVS_EOT(7),

    // 8 - Set time sent, waiting for ACK
    SET_TIME(8),

    // 9 - Set time ACK received
    SET_TIME_ACK(9),

    // 10, 11 - Setup sent
    SETUP_SENT(10),
    SETUP_SENT_WAITING_ACK(11),

    // 12 - Setup EOT
    SETUP_EOT(12),

    // 13, 14 - System status received
    SYSTEM_STATUS_RCV(13),
    SYSTEM_STATUS_WAITING_ACK(14),

    // 15, 16 - WiFi setup sent
    WIFI_SETUP_SENT(15),
    WIFI_SETUP_SENT_WAITING_ACK(16),

    // 17 - WiFi setup EOT
    WIFI_SETUP_EOT(17),

    // 18, 19 - WiFi cert sent
    WIFI_CERT_SENT(18),
    WIFI_CERT_SENT_WAITING_ACK(19),

    // 20 - WiFi cert EOT
    WIFI_CERT_EOT(20),

    // 21, 22 - Location list sent
    LOC_SENT(21),
    LOC_SENT_WAITING_ACK(22),

    // 23 - Location EOT
    LOC_EOT(23),

    // 24, 25 - Operator list sent
    OPL_SENT(24),
    OPL_SENT_WAITING_ACK(25),

    // 26 - Operator list EOT
    OPL_EOT(26),

    // 27, 28 - Patient list sent
    PTL_SENT(27),
    PTL_SENT_WAITING_ACK(28),

    // 29 - Patient list EOT
    PTL_EOT(29),

    // 30, 31 - Physician list sent
    PHYS_SENT(30),
    PHYS_SENT_WAITING_ACK(31),

    // 32 - Physician list EOT
    PHYS_EOT(32),

    // 33, 34 - Firmware sent
    FIRM_SENT(33),
    FIRM_SENT_WAITING_ACK(34),

    // 35 - Firmware EOT
    FIRM_EOT(35),

    // 36, 37 - Reagent sent
    REAG_SENT(36),
    REAG_SENT_WAITING_ACK(37),

    // 38 - Reagent EOT / Decision point
    REAG_EOT(38),

    // 39, 40 - Query states
    QUERY_SENT(39),
    QUERY_RCV(40),

    // 41 - RC command sent
    RC_COMMAND_SENT(41),

    // 42, 43 - Continuous mode
    CONTINUOUS(42),
    CONTINUOUS_ACK(43),

    // 44 - Terminate
    TERMINATE(44),

    // 45 - Firmware update sent
    FIRM_UPDATE_SENT(45);

    private final int code;

    DmlState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static DmlState fromCode(int code) {
        for (DmlState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state code: " + code);
    }
}