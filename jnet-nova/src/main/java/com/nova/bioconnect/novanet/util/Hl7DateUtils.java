package com.nova.bioconnect.novanet.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * HL7 date/time helpers.
 *
 * <p>The Bio-Connect interface uses two formats:
 * <ul>
 *   <li>{@code YYYYMMDDHHMMSS} (19 chars) for date/time of message, event, admit/discharge, observation.</li>
 *   <li>{@code YYYYMMDD} (8 chars) for date of birth.</li>
 * </ul>
 */
public final class Hl7DateUtils {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Hl7DateUtils() {}

    /** Format a date-time as {@code YYYYMMDDHHMMSS}. */
    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "" : DATETIME.format(dt);
    }

    /** Format a date as {@code YYYYMMDD}. */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** Current date-time formatted as {@code YYYYMMDDHHMMSS}. */
    public static String nowDateTime() {
        return DATETIME.format(LocalDateTime.now());
    }

    /** Parse a {@code YYYYMMDDHHMMSS} value; returns null when blank/invalid. */
    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.length() >= 14) {
                return LocalDateTime.parse(v.substring(0, 14), DATETIME);
            }
            if (v.length() >= 8) {
                return LocalDate.parse(v.substring(0, 8), DATE).atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }

    /** Parse a {@code YYYYMMDD} value; returns null when blank/invalid. */
    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || value.trim().length() < 8) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim().substring(0, 8), DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
