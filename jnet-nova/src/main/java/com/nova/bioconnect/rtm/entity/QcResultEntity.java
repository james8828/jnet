package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_qc_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sample_key_num", length = 100)
    private String sampleKeyNum;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "control_type", length = 50)
    private String controlType;

    @Column(name = "test_code", length = 64)
    private String testCode;

    @Column(name = "result_value", length = 128)
    private String resultValue;

    @Column(name = "result_units", length = 32)
    private String resultUnits;

    @Column(name = "target_value", length = 128)
    private String targetValue;

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