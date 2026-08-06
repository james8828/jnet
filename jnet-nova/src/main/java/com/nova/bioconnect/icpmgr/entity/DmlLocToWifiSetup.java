package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Location to WiFi Setup mapping entity
 * Mirrors C# DBA.loc_to_wifi_setup table
 */
@Entity
@Table(name = "dml_loc_to_wifi_setup")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLocToWifiSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "inst_class", length = 64)
    private String instClass;

    @Column(name = "config_id", length = 100)
    private String configId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
