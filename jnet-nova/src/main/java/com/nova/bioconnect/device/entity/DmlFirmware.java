package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Firmware entity
 * Mirrors C# DBA.firmware table
 */
@Entity
@Table(name = "dml_firmware")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlFirmware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firmware_id", unique = true, length = 100)
    private String firmwareId;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(name = "device_class", length = 64)
    private String deviceClass;

    @Column(name = "major_version")
    private Integer majorVersion;

    @Column(name = "minor_version")
    private Integer minorVersion;

    @Column(name = "build_num")
    private Integer buildNum;

    @Column(name = "revision")
    private Integer revision;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "region", length = 10)
    private String region;

    @Column(name = "firmware_data", columnDefinition = "TEXT")
    private String firmwareData;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "Active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
