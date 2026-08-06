package com.nova.bioconnect.icpmgr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 位置实体
 * 对应LOC消息中的位置信息
 */
@Entity
@Table(name = "dml_location")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmlLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 位置编号（主键候选）
     */
    @Column(name = "loc_num", unique = true, nullable = false, length = 50)
    private String locNum;

    /**
     * 位置名称
     */
    @Column(name = "loc_name", length = 200)
    private String locName;

    /**
     * 父位置编号
     */
    @Column(name = "parent_loc_num", length = 50)
    private String parentLocNum;

    /**
     * 层级编号（1: 设施, 2: 位置）
     */
    @Column(name = "level_num")
    private Integer levelNum;

    /**
     * 设施名称
     */
    @Column(name = "facility", length = 200)
    private String facility;

    /**
     * 位置描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 是否默认位置
     */
    @Column(name = "is_default", length = 5)
    private String isDefault;

    /**
     * 状态（Active, Inactive）
     */
    @Column(name = "status", length = 20)
    private String status;

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