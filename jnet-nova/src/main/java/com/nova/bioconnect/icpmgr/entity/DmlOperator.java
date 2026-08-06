package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 操作员实体
 * 对应OPL.R01/OPL.R02消息中的OPR节点数据
 */
@Entity
@Table(name = "dml_operator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作员ID（主键候选）
     */
    @Column(name = "operator_id", unique = true, nullable = false, length = 100)
    private String operatorId;

    /**
     * 操作员姓名
     */
    @Column(name = "operator_name", length = 200)
    private String operatorName;

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
     * 访问控制级别（1-16）
     */
    @Column(name = "access_control_level")
    private Integer accessControlLevel;

    /**
     * 权限级别
     */
    @Column(name = "privilege_level")
    private Integer privilegeLevel;

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
     * 部门
     */
    @Column(name = "department", length = 200)
    private String department;

    /**
     * 有效开始日期
     */
    @Column(name = "effective_start_dttm")
    private LocalDateTime effectiveStartDttm;

    /**
     * 有效结束日期
     */
    @Column(name = "effective_end_dttm")
    private LocalDateTime effectiveEndDttm;

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