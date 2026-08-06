package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * WiFi Credential entity
 * Mirrors C# DBA.wifi_credentials table
 */
@Entity
@Table(name = "dml_wifi_credential")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlWifiCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fac_num", length = 50)
    private String facNum;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "wifi_mac_address", length = 100)
    private String wifiMacAddress;

    @Column(name = "wifi_user_name", length = 200)
    private String wifiUserName;

    @Column(name = "wifi_password", length = 200)
    private String wifiPassword;

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
