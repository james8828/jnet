package com.nova.bioconnect.rtm.hl7;

import java.util.List;

/**
 * Encodes {@link Hl7Message} instances to their HL7 v2.4 wire representation and provides
 * escape/unescape helpers for the five special characters defined by the encoding characters
 * (MSH-2): field separator, component separator, repetition separator, escape character and
 * subcomponent separator.
 */
public final class Hl7Encoder {

    private Hl7Encoder() {}

    /** Encode a message: segments joined by {@code \r}, no trailing terminator. */
    public static String encode(Hl7Message message) {
        StringBuilder sb = new StringBuilder();
        List<Hl7Segment> segments = message.getSegments();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                sb.append(Hl7Constants.SEGMENT_TERMINATOR);
            }
            sb.append(segments.get(i).encode());
        }
        return sb.toString();
    }

    /** Encode a message, appending a trailing segment terminator. */
    public static String encodeTerminated(Hl7Message message) {
        return encode(message) + Hl7Constants.SEGMENT_TERMINATOR;
    }

    /**
     * Escape special characters in a value before placing it into an HL7 field.
     * Uses the standard HL7 escape sequences {@code \F\}, {@code \S\}, {@code \R\},
     * {@code \E\}, {@code \T\}.
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '|' -> sb.append("\\F\\");
                case '^' -> sb.append("\\S\\");
                case '~' -> sb.append("\\R\\");
                case '\\' -> sb.append("\\E\\");
                case '&' -> sb.append("\\T\\");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Reverse of {@link #escape(String)}. */
    public static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("\\F\\", "|")
                .replace("\\S\\", "^")
                .replace("\\R\\", "~")
                .replace("\\E\\", "\\")
                .replace("\\T\\", "&");
    }
}
