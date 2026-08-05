package com.nova.bioconnect.novanet.model;

/**
 * Merge information mapped from the MRG segment, used for ADT merge messages only.
 */
public record MergeInfo(
        String priorInternalPatientId,  // MRG-1
        String priorAccountNumber,      // MRG-3
        String priorExternalPatientId   // MRG-4
) {}
