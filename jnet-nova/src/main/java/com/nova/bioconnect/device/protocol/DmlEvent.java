package com.nova.bioconnect.device.protocol;

/**
 * DML Protocol Events
 * Events are triggered by received messages or internal state transitions
 */
public enum DmlEvent {

    // Message-based events (received from device)
    HEL_RECEIVED,       // HEL.R01
    DST_RECEIVED,       // DST.R01
    ACK_RECEIVED,       // ACK.R01
    OBS_RECEIVED,       // OBS.R01 or OBS.R02
    OBS_EOT_RECEIVED,   // EOT.R01 (after OBS)
    EVS_RECEIVED,       // EVS.R01
    EVS_EOT_RECEIVED,   // EOT.R01 (after EVS)
    KPA_RECEIVED,       // KPA.R01
    ESC_RECEIVED,       // ESC.R01
    END_RECEIVED,       // END.R01
    EOT_RECEIVED,       // EOT.R01 (generic)
    QUERY_RECEIVED,     // DTV.NOVA_REQ.R02
    SYSTEM_STATUS_RECEIVED, // NOVA.ANALYZER_STATE, NOVA.CARTRIDGE_STATUS, NOVA.TEST_STATUS

    // Internal transition events
    SEND_OBS_REQUEST,
    SEND_EVS_REQUEST,
    SEND_SET_TIME,
    SEND_SETUP,
    SEND_WIFI_SETUP,
    SEND_WIFI_CERT,
    SEND_LOCATION,
    SEND_OPERATOR,
    SEND_PATIENT,
    SEND_PHYSICIAN,
    SEND_FIRMWARE,
    SEND_REAGENT,
    SEND_EOT,
    SEND_CONTINUOUS,
    SEND_TERMINATE,
    SEND_KEEPALIVE,

    // Completion events
    SETUP_SENT_COMPLETE,
    WIFI_SETUP_SENT_COMPLETE,
    WIFI_CERT_SENT_COMPLETE,
    LOC_SENT_COMPLETE,
    OPL_SENT_COMPLETE,
    PTL_SENT_COMPLETE,
    PHYS_SENT_COMPLETE,
    FIRM_SENT_COMPLETE,
    REAG_SENT_COMPLETE,

    // Special events
    TIMEOUT,
    SHUTDOWN,
    NEW_MESSAGE,
    NO_OBS_TO_SEND,
    NO_EVS_TO_SEND,
    DATABASE_ERROR
}