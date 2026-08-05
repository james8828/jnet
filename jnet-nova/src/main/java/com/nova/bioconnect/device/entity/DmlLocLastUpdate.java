package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Location last update tracking entity
 * Mirrors C# DBA.loc_last_update table
 */
@Entity
@Table(name = "dml_loc_last_update",
    uniqueConstraints = @UniqueConstraint(columnNames = {"loc_num", "data_type", "inst_class", "inst_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLocLastUpdate {

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

    @PrePersist
    protected void onCreate() {
        if (lastUpdateTime == null) {
            lastUpdateTime = LocalDateTime.now();
        }
    }
}
