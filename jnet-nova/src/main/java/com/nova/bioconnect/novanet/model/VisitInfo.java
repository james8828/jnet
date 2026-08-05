package com.nova.bioconnect.novanet.model;

import java.time.LocalDateTime;

/**
 * Patient visit fields mapped from the PV1 segment (Bio-Connect interface).
 */
public record VisitInfo(
        String patientClass,          // PV1-2
        String assignedLocation,      // PV1-3.1
        String room,                  // PV1-3.2
        String bed,                   // PV1-3.3
        String facility,              // PV1-3.4
        String priorPatientLocation,  // PV1-6 (required if location is changed)
        String attendingPhysician,    // PV1-7
        String hospitalService,       // PV1-10
        String patientType,           // PV1-18
        String visitNumber,           // PV1-19
        LocalDateTime admitDateTime,  // PV1-44 (required for ADT)
        LocalDateTime dischargeDateTime // PV1-45 (required for ADT)
) {}
