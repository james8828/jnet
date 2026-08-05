package com.nova.bioconnect.novanet.model;

/**
 * A note/comment mapped from an NTE segment (Bio-Connect interface).
 */
public record Note(
        String sequenceNumber,  // NTE-1
        String commentSource,   // NTE-2 ('I' instrument, 'M' manual input)
        String comment,         // NTE-3 free text
        String commentType      // NTE-4 (always 'G')
) {}
