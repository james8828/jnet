package com.jnet.anno.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "biz_tag")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 50)
    @NotNull
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Size(max = 50)
    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "parent_id")
    private Long parentId;

    @Size(max = 20)
    @Column(name = "color_code", length = 20)
    private String colorCode;

    @ColumnDefault("0")
    @Column(name = "sort_order")
    private Integer sortOrder;

    @ColumnDefault("false")
    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "create_by")
    private Long createBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @Column(name = "update_by")
    private Long updateBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

}