package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_observation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObservationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "sample_key_num", length = 64)
    private String sampleKeyNum;

    @Column(name = "accession_num", length = 64)
    private String accessionNum;

    @Column(name = "patient_id", length = 64)
    private String patientId;

    @Column(name = "mrn", length = 64)
    private String mrn;

    @Column(name = "account_num", length = 64)
    private String accountNum;

    @Column(name = "test_cd", length = 64)
    private String testCode;

    @Column(name = "test_name", length = 128)
    private String testName;

    @Column(name = "result_value", length = 128)
    private String resultValue;

    @Column(name = "result_units", length = 32)
    private String resultUnits;

    @Column(name = "result_flags", length = 16)
    private String resultFlags;

    @Column(name = "interpretation_cd", length = 16)
    private String interpretationCd;

    @Column(name = "normal_lo_limit", length = 32)
    private String normalLoLimit;

    @Column(name = "normal_hi_limit", length = 32)
    private String normalHiLimit;

    @Column(name = "critical_lo_limit", length = 32)
    private String criticalLoLimit;

    @Column(name = "critical_hi_limit", length = 32)
    private String criticalHiLimit;

    @Column(name = "control_type", length = 32)
    private String controlType;

    @Column(name = "control_lot_num", length = 64)
    private String controlLotNum;

    @Column(name = "strip_lot_num", length = 64)
    private String stripLotNum;

    @Column(name = "observation_dttm")
    private LocalDateTime observationDttm;

    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    @Column(name = "transmitted_flag", length = 10)
    private String transmittedFlag;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}