package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Device to Lot mapping entity
 * Mirrors C# DBA.device_to_lot table
 */
@Entity
@Table(name = "dml_device_to_lot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlDeviceToLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lots_key_num", length = 100)
    private String lotsKeyNum;

    @Column(name = "inst_type", length = 64)
    private String instType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
