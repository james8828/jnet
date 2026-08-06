package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 设备事件实体
 * 对应EVS.R01消息中的EVT节点数据
 */
@Entity
@Table(name = "dml_device_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlDeviceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 事件类型代码（Main Action, Error等）
     */
    @Column(name = "event_type_cd", length = 50)
    private String eventTypeCd;

    /**
     * 事件类型（M: MT, E: SE, O: Other）
     */
    @Column(name = "event_type", length = 10)
    private String eventType;

    /**
     * 仪器编号
     */
    @Column(name = "inst_num", length = 64)
    private String instNum;

    /**
     * UUID
     */
    @Column(name = "uuid", length = 64)
    private String uuid;

    /**
     * 归档标志
     */
    @Column(name = "arch", length = 5)
    private String arch;

    /**
     * 事件日期时间
     */
    @Column(name = "event_dttm")
    private LocalDateTime eventDttm;

    /**
     * 事件描述
     */
    @Column(name = "event_desc", length = 500)
    private String eventDesc;

    /**
     * 事件严重级别（I: 信息, W: 警告, E: 错误, F: 致命）
     */
    @Column(name = "severity_cd", length = 5)
    private String severityCd;

    /**
     * 事件代码
     */
    @Column(name = "event_code", length = 100)
    private String eventCode;

    /**
     * 事件状态（Active, Resolved等）
     */
    @Column(name = "event_status", length = 20)
    private String eventStatus;

    /**
     * 设备序列号
     */
    @Column(name = "device_serial_id", length = 100)
    private String deviceSerialId;

    /**
     * 操作员ID
     */
    @Column(name = "operator_id", length = 100)
    private String operatorId;

    /**
     * 操作员姓名
     */
    @Column(name = "operator_name", length = 200)
    private String operatorName;

    /**
     * 设施
     */
    @Column(name = "facility", length = 200)
    private String facility;

    /**
     * 位置
     */
    @Column(name = "location", length = 200)
    private String location;

    /**
     * 原始XML文本
     */
    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}