package com.nova.bioconnect.novanet.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated model for an unsolicited observation result message
 * (ORU^R01 for patient results, OUL^R21 for QC results).
 *
 * <p>Only one patient (or one QC specimen) is sent per message. Patient and QC fields are
 * mutually exclusive: {@code isQc=true} indicates a QC result (OUL^R21) carrying {@link #qcInfo()};
 * otherwise a patient result (ORU^R01) carries {@link #patient()}.
 */
public record ResultMessage(
        boolean isQc,                       // true = OUL^R21 (QC), false = ORU^R01 (patient)
        PatientInfo patient,                 // PID (nullable for QC)
        VisitInfo visit,                     // PV1 (optional)
        QcInfo qcInfo,                       // SAC/SID (nullable for patient)
        // ORC - common order
        String fillerOrderNumber,            // ORC-3 (unique, created by Bio-Connect)
        LocalDateTime transactionDateTime,   // ORC-9
        // OBR - observation request
        String obrSetId,                     // OBR-1 (always 1)
        String placerOrder,                  // OBR-2 (sample/accession id)
        String serviceIdentifier,            // OBR-4 (profile name, default WGLUC)
        LocalDateTime observationDateTime,   // OBR-7
        LocalDateTime observationEndDateTime,// OBR-8
        LocalDateTime specimenReceivedDateTime, // OBR-14
        String specimenSource,               // OBR-15
        String orderingProvider,             // OBR-16
        String resultStatus,                 // OBR-25 (always 'F')
        // notes and results
        List<Note> notes,                    // {[NTE]} before/around OBX
        List<ObservationResult> results      // {OBX {[NTE]}}
) {}
