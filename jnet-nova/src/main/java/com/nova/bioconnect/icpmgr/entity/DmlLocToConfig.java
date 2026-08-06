package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Location to Config mapping entity
 * Mirrors C# DBA.loc_to_config table
 */
@Entity
@Table(name = "dml_loc_to_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLocToConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "config_num", length = 100)
    private String configNum;

    @Column(name = "inst_type", length = 64)
    private String instType;

    @Column(name = "inst_class", length = 64)
    private String instClass;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
