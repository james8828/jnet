package com.nova.bioconnect.rtm.model;

/**
 * A single observation result mapped from an OBX segment (Bio-Connect interface).
 */
public record ObservationResult(
        String sequenceNumber,     // OBX-1
        String valueType,          // OBX-2 (NM numeric, ST non-numeric)
        String testCode,           // OBX-3.1
        String testName,           // OBX-3.2
        String resultValue,        // OBX-5
        String units,              // OBX-6
        String referenceRange,     // OBX-7
        String abnormalFlags,      // OBX-8 (''/'N', 'H', 'L', 'A', '<', '>')
        String resultStatus,       // OBX-11 ('F', 'P', 'V', 'W')
        String responsibleObserver // OBX-16 (operator ID)
) {}
