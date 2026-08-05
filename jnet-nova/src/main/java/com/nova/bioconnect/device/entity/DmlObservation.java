package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DML Observation Entity
 * Stores observation results from devices
 */
@Entity
@Table(name = "dml_observation", indexes = {
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_sample_key", columnList = "sample_key_num"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmlObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to device
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DmlDevice device;

    /**
     * Sample key number
     */
    @Column(name = "sample_key_num", length = 64)
    private String sampleKeyNum;

    /**
     * Accession number
     */
    @Column(name = "accession_num", length = 64)
    private String accessionNum;

    /**
     * Patient ID
     */
    @Column(name = "patient_id", length = 64)
    private String patientId;

    /**
     * Medical record number
     */
    @Column(name = "mrn", length = 64)
    private String mrn;

    /**
     * Account number
     */
    @Column(name = "account_num", length = 64)
    private String accountNum;

    /**
     * Test code / observation ID
     */
    @Column(name = "test_cd", length = 64)
    private String testCd;

    /**
     * Result value
     */
    @Column(name = "result_value", length = 128)
    private String resultValue;

    /**
     * Result units
     */
    @Column(name = "result_units", length = 32)
    private String resultUnits;

    /**
     * Result flags (e.g., H, L, N)
     */
    @Column(name = "result_flags", length = 16)
    private String resultFlags;

    /**
     * Interpretation code
     */
    @Column(name = "interpretation_cd", length = 16)
    private String interpretationCd;

    /**
     * Normal low limit
     */
    @Column(name = "normal_lo_limit", length = 32)
    private String normalLoLimit;

    /**
     * Normal high limit
     */
    @Column(name = "normal_hi_limit", length = 32)
    private String normalHiLimit;

    /**
     * Critical low limit
     */
    @Column(name = "critical_lo_limit", length = 32)
    private String criticalLoLimit;

    /**
     * Critical high limit
     */
    @Column(name = "critical_hi_limit", length = 32)
    private String criticalHiLimit;

    /**
     * Control type
     */
    @Column(name = "control_type", length = 32)
    private String controlType;

    /**
     * Control lot number
     */
    @Column(name = "control_lot_num", length = 64)
    private String controlLotNum;

    /**
     * Strip lot number
     */
    @Column(name = "strip_lot_num", length = 64)
    private String stripLotNum;

    /**
     * Observation date/time
     */
    @Column(name = "observation_dttm")
    private LocalDateTime observationDttm;

    /**
     * Full XML text of the observation
     */
    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    /**
     * Transmitted flag
     */
    @Column(name = "transmitted_flag")
    @Builder.Default
    private String transmittedFlag = "F";

    /**
     * Created at
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}