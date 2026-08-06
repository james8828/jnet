package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_sample_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sample_key_num", unique = true, length = 100)
    private String sampleKeyNum;

    @Column(name = "device_serial_id", length = 100)
    private String deviceSerial;

    @Column(name = "sample_date")
    private LocalDateTime sampleDate;

    @Column(name = "transmitted_flag", length = 10)
    private String transmittedFlag;

    @Column(name = "saved_to_history_db_flag", length = 10)
    private String savedToHistoryDbFlag;

    @Column(name = "control_type", length = 50)
    private String controlType;

    @Column(name = "accession_num", length = 100)
    private String accessionNum;

    @Column(name = "control_lot_num", length = 100)
    private String controlLotNum;

    @Column(name = "strip_lot_num", length = 100)
    private String stripLotNum;

    @Column(name = "lot_level", length = 50)
    private String lotLevel;

    @Column(name = "internal_external", length = 20)
    private String internalExternal;

    @Column(name = "patient_id", length = 100)
    private String patientNum;

    @Column(name = "medrec_num", length = 100)
    private String medrecNum;

    @Column(name = "account_num", length = 100)
    private String accountNum;

    @Column(name = "enterprise_id", length = 100)
    private String enterpriseId;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "loc_name", length = 200)
    private String locName;

    @Column(name = "fac_name", length = 200)
    private String facName;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(name = "device_name", length = 128)
    private String deviceName;

    @Column(name = "device_sw_ver", length = 32)
    private String deviceSwVer;

    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    @Column(name = "is_qc")
    private Boolean isQc;

    @Column(name = "sample_id_type", length = 20)
    private String sampleIdType;

    @Column(name = "observation_id", length = 100)
    private String observationId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}