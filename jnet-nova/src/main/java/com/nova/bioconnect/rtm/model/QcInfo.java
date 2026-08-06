package com.nova.bioconnect.rtm.model;

/**
 * Quality-control specimen/substance information mapped from SAC and SID segments
 * (Bio-Connect interface). Only present for QC result messages (OUL^R21).
 */
public record QcInfo(
        String containerIdentifier,      // SAC-3
        String specimenSource,           // SAC-6
        String lotNumber,                // SID-2
        String lotLevel,                 // SID-3
        String manufacturerIdentifier    // SID-4
) {}
