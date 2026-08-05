package com.nova.bioconnect.novanet.model;

import java.time.LocalDate;

/**
 * Patient identification fields mapped from the PID segment (Bio-Connect interface).
 *
 * <p>Either {@code externalPatientId} (PID-2) or {@code internalPatientId} (PID-3, MRN) must be
 * provided when available.
 */
public record PatientInfo(
        String externalPatientId,   // PID-2
        String internalPatientId,   // PID-3 (MRN)
        String lastName,            // PID-5.1
        String firstName,           // PID-5.2
        String middleName,          // PID-5.3
        String prefix,              // PID-5.4
        String suffix,              // PID-5.5
        LocalDate dateOfBirth,      // PID-7
        String gender,              // PID-8 (M, F, U)
        String race,                // PID-9
        String address,             // PID-11
        String phoneHome,           // PID-13
        String accountNumber        // PID-18
) {}
