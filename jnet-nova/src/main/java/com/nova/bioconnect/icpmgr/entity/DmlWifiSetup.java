package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * WiFi Setup entity
 * Mirrors C# DBA.wifi_setup table
 */
@Entity
@Table(name = "dml_wifi_setup")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlWifiSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", unique = true, length = 100)
    private String configId;

    @Column(name = "wifi_data", columnDefinition = "TEXT")
    private String wifiData;

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
