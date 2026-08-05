package com.nova.bioconnect.novanet.hl7;

/**
 * Parser for HL7 v2.4 messages.
 *
 * <p>Accepts the raw message payload extracted from an MLLP frame (i.e. without the
 * MLLP start/end-block characters) and produces an {@link Hl7Message}. Segments are
 * separated by {@code \r}; {@code \n} and {@code \r\n} are tolerated as well.
 */
public final class Hl7Parser {

    private Hl7Parser() {}

    /** Parse raw HL7 text into a message. Blank lines are ignored. */
    public static Hl7Message parse(String raw) {
        Hl7Message message = new Hl7Message();
        if (raw == null || raw.isEmpty()) {
            return message;
        }
        // Normalise line endings to \r then split.
        String normalised = raw.replace("\r\n", "\r").replace("\n", "\r");
        String[] lines = normalised.split("\r", -1);
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            // Skip stray MLLP control chars that may have leaked through.
            String cleaned = line
                    .replace("\u000B", "")
                    .replace("\u001C", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            message.addSegment(Hl7Segment.parse(cleaned));
        }
        return message;
    }

    /** True when the message has an MSH segment and a non-empty message type (MSH-9). */
    public static boolean isValid(Hl7Message message) {
        if (message == null || message.getMsh() == null) {
            return false;
        }
        return !message.getMessageType().isEmpty();
    }
}
