package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dml_patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_num", unique = true, length = 100)
    private String patientNum;

    @Column(name = "patient_id", length = 100)
    private String patientId;

    @Column(name = "medrec_num", length = 100)
    private String medrecNum;

    @Column(name = "account_num", length = 100)
    private String accountNum;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "sex", length = 5)
    private String sex;

    @Column(name = "race", length = 50)
    private String race;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone_home", length = 40)
    private String phoneHome;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "location", length = 200)
    private String location;

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