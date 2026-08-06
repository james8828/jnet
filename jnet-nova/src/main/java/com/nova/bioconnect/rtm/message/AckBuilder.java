package com.nova.bioconnect.rtm.message;

import com.nova.bioconnect.rtm.hl7.Hl7Constants;
import com.nova.bioconnect.rtm.hl7.Hl7Message;
import com.nova.bioconnect.rtm.hl7.Hl7Segment;
import com.nova.bioconnect.rtm.util.Hl7DateUtils;

/**
 * Builds HL7 acknowledgement messages (MSH + MSA) as defined by the Bio-Connect interface.
 *
 * <p>The acknowledgement echoes the MESSAGE CONTROL ID of the original message in both MSH-10
 * and MSA-2, and uses MSA-1 to indicate positive ({@code AA}) or negative ({@code AE}/{@code AR}).
 *
 * <p>Example (Bio-Connect sent):
 * <pre>
 * MSH|^~\&amp;|||||20090217181020||ACK|MT-&gt;COBAS.1.446736|P|2.4
 * MSA|AA|MT-&gt;COBAS.1.446736||
 * </pre>
 */
public final class AckBuilder {

    private AckBuilder() {}

    /**
     * Build an ACK for the given original message.
     *
     * @param original the message being acknowledged
     * @param ackCode  one of {@link Hl7Constants#ACK_AA}, {@link Hl7Constants#ACK_AE}, {@link Hl7Constants#ACK_AR}
     */
    public static Hl7Message build(Hl7Message original, String ackCode) {
        return build(original, ackCode, Hl7Constants.PROCESSING_PRODUCTION, Hl7Constants.HL7_VERSION);
    }

    /** Build an ACK with explicit processing id and version. */
    public static Hl7Message build(Hl7Message original, String ackCode, String processingId, String version) {
        String controlId = original == null ? "" : original.getMessageControlId();
        return build(controlId, ackCode, processingId, version);
    }

    /** Build an ACK from the original message control id directly. */
    public static Hl7Message build(String originalControlId, String ackCode, String processingId, String version) {
        String code = (ackCode == null || ackCode.isEmpty()) ? Hl7Constants.ACK_AA : ackCode;
        String now = Hl7DateUtils.nowDateTime();

        Hl7Segment msh = Hl7Segment.msh(Hl7Constants.ENCODING_CHARS,
                "",                       // MSH-3 sending application
                "",                       // MSH-4 sending facility
                "",                       // MSH-5 receiving application
                "",                       // MSH-6 receiving facility
                now,                      // MSH-7 date/time of message
                "",                       // MSH-8 security
                Hl7Constants.MSG_ACK,     // MSH-9 message type
                originalControlId,        // MSH-10 message control id (echo original)
                processingId,             // MSH-11 processing id
                version                   // MSH-12 version id
        );

        Hl7Segment msa = Hl7Segment.of(Hl7Constants.MSA,
                code,                     // MSA-1 acknowledgement code (AA / AE / AR)
                originalControlId         // MSA-2 message control id (echo original)
        );

        return new Hl7Message().addSegment(msh).addSegment(msa);
    }
}
