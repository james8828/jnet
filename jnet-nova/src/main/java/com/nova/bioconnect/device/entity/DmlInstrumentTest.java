package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Instrument Test entity
 * Mirrors C# DBA.instruments_tests table
 */
@Entity
@Table(name = "dml_instrument_test")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlInstrumentTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inst_type", length = 64)
    private String instType;

    @Column(name = "inst_class", length = 64)
    private String instClass;

    @Column(name = "test_name", length = 100)
    private String testName;

    @Column(name = "generic_test_name", length = 100)
    private String genericTestName;

    @Column(name = "test_code", length = 64)
    private String testCode;

    @Column(name = "test_code_system", length = 32)
    private String testCodeSystem;

    @Column(name = "result_type_code", length = 10)
    private String resultTypeCode;

    @Column(name = "units", length = 32)
    private String units;

    @Column(name = "units_of_measure", length = 32)
    private String unitsOfMeasure;

    @Column(name = "lo_limit", length = 32)
    private String loLimit;

    @Column(name = "hi_limit", length = 32)
    private String hiLimit;

    @Column(name = "lo_panic_limit", length = 32)
    private String loPanicLimit;

    @Column(name = "hi_panic_limit", length = 32)
    private String hiPanicLimit;

    @Column(name = "lo_normal_limit", length = 32)
    private String loNormalLimit;

    @Column(name = "hi_normal_limit", length = 32)
    private String hiNormalLimit;

    @Column(name = "sex", length = 5)
    private String sex;

    @Column(name = "age_type", length = 10)
    private String ageType;

    @Column(name = "age_lo")
    private Integer ageLo;

    @Column(name = "age_hi")
    private Integer ageHi;

    @Column(name = "enable_all_ages", length = 5)
    private String enableAllAges;

    @Column(name = "range_label", length = 100)
    private String rangeLabel;

    @Column(name = "equation", length = 200)
    private String equation;

    @Column(name = "eq_const", length = 100)
    private String eqConst;

    @Column(name = "enable_deselect", length = 5)
    private String enableDeselect;

    @Column(name = "ui_order")
    private Integer uiOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enableAllAges == null) enableAllAges = "T";
        if (enableDeselect == null) enableDeselect = "T";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
