package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Lot Chemistry entity
 * Mirrors C# DBA.lot_chem table
 */
@Entity
@Table(name = "dml_lot_chem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLotChem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lots_key_num", length = 100)
    private String lotsKeyNum;

    @Column(name = "generic_test_name", length = 100)
    private String genericTestName;

    @Column(name = "test_name", length = 100)
    private String testName;

    @Column(name = "observation_id", length = 100)
    private String observationId;

    @Column(name = "lo_limit", length = 32)
    private String loLimit;

    @Column(name = "hi_limit", length = 32)
    private String hiLimit;

    @Column(name = "units", length = 32)
    private String units;

    @Column(name = "facility_num", length = 50)
    private String facilityNum;

    @Column(name = "level_number", length = 20)
    private String levelNumber;

    @Column(name = "level_type", length = 50)
    private String levelType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
