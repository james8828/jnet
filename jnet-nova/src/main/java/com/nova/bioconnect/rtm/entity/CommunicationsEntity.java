package com.nova.bioconnect.rtm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dml_communication")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "computer_name", length = 100)
    private String computerName;

    @Column(name = "instrument_id", length = 100)
    private String instrumentId;

    @Column(name = "port_num")
    private Integer portNum;

    @Column(name = "port_type", length = 50)
    private String portType;

    @Column(name = "comm_record_num", length = 100)
    private String commRecordNum;

    @Column(name = "started_dttm")
    private LocalDateTime startedDttm;

    @Column(name = "last_comm_dttm")
    private LocalDateTime lastCommDttm;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}