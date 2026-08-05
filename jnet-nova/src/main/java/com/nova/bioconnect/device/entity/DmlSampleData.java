package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 样本数据实体
 * 对应C# Sample_Table结构（line 77-101）
 */
@Entity
@Table(name = "dml_sample_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlSampleData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 样本唯一标识
     */
    @Column(name = "sample_key_num", length = 100)
    private String sampleKeyNum;

    /**
     * 访问号
     */
    @Column(name = "accession_num", length = 100)
    private String accessionNum;

    /**
     * 样本日期时间
     */
    @Column(name = "sample_date")
    private LocalDateTime sampleDate;

    /**
     * 传输标志
     */
    @Column(name = "transmitted_flag", length = 10)
    private String transmittedFlag;

    /**
     * 控制类型（QC, Patient等）
     */
    @Column(name = "control_type", length = 50)
    private String controlType;

    /**
     * 控制批号
     */
    @Column(name = "control_lot_num", length = 100)
    private String controlLotNum;

    /**
     * 试纸批号
     */
    @Column(name = "strip_lot_num", length = 100)
    private String stripLotNum;

    /**
     * 批次级别
     */
    @Column(name = "lot_level", length = 50)
    private String lotLevel;

    /**
     * 内外部标志（Internal/External）
     */
    @Column(name = "internal_external", length = 20)
    private String internalExternal;

    /**
     * 原始XML文本
     */
    @Column(name = "xml_text", columnDefinition = "TEXT")
    private String xmlText;

    /**
     * 患者ID
     */
    @Column(name = "patient_id", length = 100)
    private String patientId;

    /**
     * 病历号
     */
    @Column(name = "medrec_num", length = 100)
    private String medrecNum;

    /**
     * 账号
     */
    @Column(name = "account_num", length = 100)
    private String accountNum;

    /**
     * 企业ID
     */
    @Column(name = "enterprise_id", length = 100)
    private String enterpriseId;

    /**
     * 设备序列号
     */
    @Column(name = "device_serial_id", length = 100)
    private String deviceSerialId;

    /**
     * 设施名称 (fac_name)
     */
    @Column(name = "facility", length = 200)
    private String facility;

    /**
     * 位置名称 (loc_name)
     */
    @Column(name = "location", length = 200)
    private String location;

    /**
     * 设备类型
     */
    @Column(name = "device_type", length = 64)
    private String deviceType;

    /**
     * 设备名称
     */
    @Column(name = "device_name", length = 128)
    private String deviceName;

    /**
     * 设备软件版本
     */
    @Column(name = "device_sw_ver", length = 32)
    private String deviceSwVer;

    /**
     * 位置编号
     */
    @Column(name = "loc_num", length = 50)
    private String locNum;

    /**
     * 操作员ID
     */
    @Column(name = "operator_id", length = 100)
    private String operatorId;

    /**
     * 是否QC样本
     */
    @Column(name = "is_qc")
    private Boolean isQc;

    /**
     * 样本ID类型（PATID, MRN, ACCT）
     */
    @Column(name = "sample_id_type", length = 20)
    private String sampleIdType;

    /**
     * 观察ID（observation_id）
     */
    @Column(name = "observation_id", length = 100)
    private String observationId;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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