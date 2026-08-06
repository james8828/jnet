package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_patient_visit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientVisitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_num", unique = true, length = 100)
    private String visitNum;

    @Column(name = "visit_number", length = 100)
    private String visitNumber;

    @Column(name = "patient_num", length = 100)
    private String patientNum;

    @Column(name = "account_num", length = 100)
    private String accountNum;

    @Column(name = "visit_type", length = 50)
    private String visitType;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "room", length = 50)
    private String room;

    @Column(name = "bed", length = 50)
    private String bed;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "admitting_doctor", length = 200)
    private String admittingDoctor;

    @Column(name = "visit_date")
    private LocalDateTime visitDate;

    @Column(name = "discharging_date")
    private LocalDateTime dischargingDate;

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
        if (status == null) status = "A";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}