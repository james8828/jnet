package com.nova.bioconnect.device.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者实体
 * 对应PTL.R01/PTL.R02消息中的PT节点数据
 */
@Entity
@Table(name = "dml_patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 患者ID（主键候选）
     */
    @Column(name = "patient_id", unique = true, nullable = false, length = 100)
    private String patientId;

    /**
     * 病历号
     */
    @Column(name = "medrec_num", length = 100)
    private String medrecNum;

    /**
     * 企业ID
     */
    @Column(name = "enterprise_id", length = 100)
    private String enterpriseId;

    /**
     * 账号
     */
    @Column(name = "account_num", length = 100)
    private String accountNum;

    /**
     * 患者姓名
     */
    @Column(name = "patient_name", length = 200)
    private String patientName;

    /**
     * 名
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * 姓
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * 中间名
     */
    @Column(name = "middle_name", length = 100)
    private String middleName;

    /**
     * 出生日期
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * 性别（M: 男, F: 女, U: 未知）
     */
    @Column(name = "sex", length = 5)
    private String sex;

    /**
     * 种族
     */
    @Column(name = "race", length = 50)
    private String race;

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
     * 床号
     */
    @Column(name = "bed", length = 50)
    private String bed;

    /**
     * 房间
     */
    @Column(name = "room", length = 50)
    private String room;

    /**
     * 诊断代码
     */
    @Column(name = "diagnosis_code", length = 100)
    private String diagnosisCode;

    /**
     * 诊断描述
     */
    @Column(name = "diagnosis_desc", length = 500)
    private String diagnosisDesc;

    /**
     * 医生ID
     */
    @Column(name = "physician_id", length = 100)
    private String physicianId;

    /**
     * 医生姓名
     */
    @Column(name = "physician_name", length = 200)
    private String physicianName;

    /**
     * 状态（Active, Inactive）
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 备注
     */
    @Column(name = "note", length = 1000)
    private String note;

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
        if (status == null) {
            status = "Active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}