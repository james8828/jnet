package com.nova.bioconnect.novanet.hl7;

import java.util.Set;

/**
 * HL7 v2.4 constants used by the Bio-Connect interface.
 *
 * <p>Field positions, segment names, message types and acknowledgement codes are
 * derived from the Bio-Connect HL7 Interface specification (v1.5).
 */
public final class Hl7Constants {

    private Hl7Constants() {}

    // ---- MLLP / encoding characters (MSH-1, MSH-2) ----
    /** MLLP start-block character: 0x0B (Vertical Tab). */
    public static final byte MLLP_START = 0x0B;
    /** MLLP end-block character: 0x1C (File Separator). */
    public static final byte MLLP_END = 0x1C;
    /** MLLP trailing carriage return: 0x0D. */
    public static final byte MLLP_CR = 0x0D;

    public static final String FIELD_SEPARATOR = "|";
    public static final String ENCODING_CHARS = "^~\\&";
    public static final char COMPONENT_SEP = '^';
    public static final char REPETITION_SEP = '~';
    public static final char ESCAPE_CHAR = '\\';
    public static final char SUBCOMPONENT_SEP = '&';
    /** HL7 segment terminator. */
    public static final String SEGMENT_TERMINATOR = "\r";

    // ---- Segment names ----
    public static final String MSH = "MSH";
    public static final String MSA = "MSA";
    public static final String EVN = "EVN";
    public static final String PID = "PID";
    public static final String PV1 = "PV1";
    public static final String MRG = "MRG";
    public static final String ORC = "ORC";
    public static final String OBR = "OBR";
    public static final String OBX = "OBX";
    public static final String NTE = "NTE";
    public static final String SAC = "SAC";
    public static final String SID = "SID";

    // ---- Message types (MSH-9) ----
    public static final String MSG_ACK = "ACK";
    public static final String MSG_ADT = "ADT";
    public static final String MSG_ORU_R01 = "ORU^R01";   // unsolicited patient observation/result
    public static final String MSG_OUL_R21 = "OUL^R21";   // unsolicited QC observation/result

    // ---- Acknowledgement codes (MSA-1) ----
    public static final String ACK_AA = "AA";  // positive
    public static final String ACK_AE = "AE";  // application error (negative)
    public static final String ACK_AR = "AR";  // application reject

    // ---- Processing id (MSH-11) ----
    public static final String PROCESSING_PRODUCTION = "P";
    public static final String PROCESSING_DEBUG = "D";

    // ---- Result status ----
    public static final String RESULT_STATUS_FINAL = "F";
    public static final String RESULT_STATUS_PRELIMINARY = "P";
    public static final String RESULT_STATUS_VERIFIED = "V";

    // ---- Order control / status ----
    public static final String ORDER_CONTROL_NEW = "NW";
    public static final String ORDER_STATUS_COMPLETE = "CM";
    public static final String PLACER_ORDER_HIS = "^HIS";

    // ---- NTE comment type / source ----
    public static final String NTE_COMMENT_TYPE = "G";
    public static final String NTE_SOURCE_INSTRUMENT = "I";
    public static final String NTE_SOURCE_MANUAL = "M";

    // ---- OBX value types ----
    public static final String VALUE_TYPE_NM = "NM";  // numeric
    public static final String VALUE_TYPE_ST = "ST";  // string (non-numeric)

    // ---- Abnormal flags (OBX-8) ----
    public static final String ABNORMAL_NORMAL = "N";
    public static final String ABNORMAL_HIGH = "H";
    public static final String ABNORMAL_LOW = "L";
    public static final String ABNORMAL_A = "A";
    public static final String ABNORMAL_BELOW = "<";
    public static final String ABNORMAL_ABOVE = ">";

    /**
     * ADT trigger events explicitly ignored by the Bio-Connect ADT interface
     * (per section: "the following are ignored by Bio-Connect ADT interface").
     */
    public static final Set<String> IGNORED_ADT_TRIGGERS = Set.of(
            "A14", "A15", "A16", "A20", "A24", "A25", "A26", "A27", "A28", "A29",
            "A30", "A32", "A37", "A39", "A42", "A43", "A44", "A45", "A46", "A48",
            "A50", "A51", "A54", "A55", "A60", "A61", "A62");

    /** ADR^A19 query is not supported. */
    public static final String IGNORED_ADR_TRIGGER = "A19";

    /** HL7 version supported. */
    public static final String HL7_VERSION = "2.4";

    /** Default service identifier used by Bio-Connect when none provided (OBR-4). */
    public static final String DEFAULT_SERVICE_IDENTIFIER = "WGLUC";
}
