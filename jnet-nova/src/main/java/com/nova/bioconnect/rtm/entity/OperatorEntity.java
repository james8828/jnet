package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_operator")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_num", unique = true, length = 100)
    private String operatorNum;

    @Column(name = "operator_id", length = 100)
    private String operatorId;

    @Column(name = "operator_name", length = 200)
    private String operatorName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "is_supervisor", length = 1)
    private String isSupervisor;

    @Column(name = "privilege_level")
    private Integer privilegeLevel;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "loc_num", length = 50)
    private String locNum;

    @Column(name = "department", length = 200)
    private String department;

    @Column(name = "effective_start_dttm")
    private LocalDateTime effectiveStartDttm;

    @Column(name = "effective_end_dttm")
    private LocalDateTime effectiveEndDttm;

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