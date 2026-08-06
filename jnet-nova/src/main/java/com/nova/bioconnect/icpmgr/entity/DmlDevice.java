package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DML Device Entity
 * Stores device information from DML protocol
 */
@Entity
@Table(name = "dml_device")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmlDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Device serial number (unique)
     */
    @Column(name = "serial_id", unique = true, nullable = false, length = 64)
    private String serialId;

    /**
     * Device name
     */
    @Column(name = "device_name", length = 128)
    private String deviceName;

    /**
     * Device type (e.g., StatStrip)
     */
    @Column(name = "device_type", length = 64)
    private String deviceType;

    /**
     * Device class
     */
    @Column(name = "device_class", length = 64)
    private String deviceClass;

    /**
     * From instrument ID
     */
    @Column(name = "from_inst_id", length = 64)
    private String fromInstId;

    /**
     * Vendor ID
     */
    @Column(name = "vendor_id", length = 64)
    private String vendorId;

    /**
     * Software version
     */
    @Column(name = "sw_version", length = 32)
    private String swVersion;

    /**
     * Hardware version
     */
    @Column(name = "hw_version", length = 32)
    private String hwVersion;

    /**
     * Location number
     */
    @Column(name = "loc_num", length = 64)
    private String locNum;

    /**
     * Facility name
     */
    @Column(name = "fac_name", length = 128)
    private String facName;

    /**
     * Instrument number
     */
    @Column(name = "inst_num", length = 64)
    private String instNum;

    /**
     * Supports set time
     */
    @Column(name = "supports_set_time")
    @Builder.Default
    private Boolean supportsSetTime = false;

    /**
     * Supports continuous mode
     */
    @Column(name = "supports_continuous")
    @Builder.Default
    private Boolean supportsContinuous = false;

    /**
     * Is in continuous mode
     */
    @Column(name = "is_continuous")
    @Builder.Default
    private Boolean isContinuous = false;

    /**
     * Last communication time
     */
    @Column(name = "last_comm_dttm")
    private LocalDateTime lastCommDttm;

    /**
     * Created at
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Updated at
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

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