package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Configuration data entity
 * Mirrors C# DBA.config_data table
 */
@Entity
@Table(name = "dml_config_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlConfigData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_num", unique = true, length = 100)
    private String configNum;

    @Column(name = "config_key", length = 200)
    private String configKey;

    @Column(name = "config_value", length = 2000)
    private String configValue;

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
