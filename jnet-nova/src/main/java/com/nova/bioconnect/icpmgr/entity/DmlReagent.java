package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 试剂实体
 * 对应NOVA.REAG消息中的试剂信息
 */
@Entity
@Table(name = "dml_reagent")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlReagent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 试剂编号
     */
    @Column(name = "reagent_num", length = 50)
    private String reagentNum;

    /**
     * 试剂名称
     */
    @Column(name = "reagent_name", length = 200)
    private String reagentName;

    /**
     * 试剂类型（QC: 质控, REAG: 试剂, PF: 比对等）
     */
    @Column(name = "reagent_type", length = 20)
    private String reagentType;

    /**
     * 批号
     */
    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    /**
     * 级别
     */
    @Column(name = "level_cd", length = 20)
    private String levelCd;

    /**
     * 有效期开始
     */
    @Column(name = "valid_start_date")
    private LocalDate validStartDate;

    /**
     * 有效期结束
     */
    @Column(name = "valid_end_date")
    private LocalDate validEndDate;

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
     * 设备类型
     */
    @Column(name = "inst_type", length = 50)
    private String instType;

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