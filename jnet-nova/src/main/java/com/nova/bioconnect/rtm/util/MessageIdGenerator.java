package com.nova.bioconnect.rtm.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique HL7 message control ids (MSH-10) for messages originated by Bio-Connect.
 *
 * <p>Format: {@code BC<timestamp><counter>}, e.g. {@code BC20090324164304001}.
 */
@Component
public class MessageIdGenerator {

    private final AtomicLong counter = new AtomicLong(0);

    public String next() {
        long seq = counter.incrementAndGet();
        return "BC" + Hl7DateUtils.nowDateTime() + String.format("%03d", seq % 1000);
    }

    /** Build a control id using the current time and an incrementing counter. */
    public String next(String prefix) {
        long seq = counter.incrementAndGet();
        return prefix + Hl7DateUtils.nowDateTime() + String.format("%03d", seq % 1000);
    }
}
