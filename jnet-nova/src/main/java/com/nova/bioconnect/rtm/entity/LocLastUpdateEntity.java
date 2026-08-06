package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_loc_last_update")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocLastUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loc_num", nullable = false, length = 50)
    private String locNum;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;

    @Column(name = "inst_class", length = 64)
    private String instClass;

    @Column(name = "inst_type", length = 64)
    private String instType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}