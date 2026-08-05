package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Physician entity
 * Mirrors C# DBA.physicians table
 */
@Entity
@Table(name = "dml_physician")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlPhysician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "physician_id", unique = true, nullable = false, length = 100)
    private String physicianId;

    @Column(name = "physician_name", length = 200)
    private String physicianName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Column(name = "suffix", length = 20)
    private String suffix;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "department", length = 200)
    private String department;

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
