package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Order entity
 * Mirrors C# DBA.orders table
 */
@Entity
@Table(name = "dml_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accession_num", unique = true, length = 100)
    private String accessionNum;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "patient_id", length = 100)
    private String patientId;

    @Column(name = "device_serial_id", length = 100)
    private String deviceSerialId;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "ordering_provider_id", length = 100)
    private String orderingProviderId;

    @Column(name = "universal_service_id", length = 100)
    private String universalServiceId;

    @Column(name = "order_status", length = 20)
    private String orderStatus;

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
