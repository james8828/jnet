package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reagent Lot entity
 * Mirrors C# DBA.lots table
 */
@Entity
@Table(name = "dml_lot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lots_key_num", unique = true, length = 100)
    private String lotsKeyNum;

    @Column(name = "lot", length = 100)
    private String lot;

    @Column(name = "lot_type", length = 50)
    private String lotType;

    @Column(name = "lot_name", length = 100)
    private String lotName;

    @Column(name = "exp_date")
    private LocalDate expDate;

    @Column(name = "datetime_stamp")
    private LocalDateTime datetimeStamp;

    @Column(name = "in_use", length = 5)
    private String inUse;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "retired", length = 5)
    private String retired;

    @Column(name = "is_validated", length = 5)
    private String isValidated;

    @Column(name = "level_cd", length = 20)
    private String levelCd;

    @Column(name = "level_type", length = 50)
    private String levelType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (inUse == null) inUse = "T";
        if (retired == null) retired = "F";
        if (isValidated == null) isValidated = "F";
        if (usedCount == null) usedCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
