package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_location")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loc_num", unique = true, nullable = false, length = 50)
    private String locNum;

    @Column(name = "loc_name", length = 200)
    private String locName;

    @Column(name = "parent_loc_num", length = 50)
    private String parent;

    @Column(name = "level_num")
    private Integer levelNum;

    @Column(name = "facility", length = 200)
    private String facility;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_default", length = 5)
    private String isDefault;

    @Column(name = "inst_class", length = 64)
    private String instClass;

    @Column(name = "inst_type", length = 64)
    private String instType;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}