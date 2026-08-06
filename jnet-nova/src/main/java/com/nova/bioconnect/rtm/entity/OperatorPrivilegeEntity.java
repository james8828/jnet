package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_operator_privilege")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorPrivilegeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_num", length = 100)
    private String operatorNum;

    @Column(name = "inst_type", length = 64)
    private String instType;

    @Column(name = "privilege_code", length = 50)
    private String privilegeCode;

    @Column(name = "privilege_desc", length = 500)
    private String privilegeDesc;

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