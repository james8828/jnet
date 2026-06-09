-- ============================================================================
-- 病理AI数据池管理系统 - 数据库建表脚本
-- 数据库: PostgreSQL 14+
-- 扩展: PostGIS (用于矢量标注空间数据存储)
-- 创建日期: 2024-04-16
-- 更新日期: 2026-05-06
-- 
-- 重要说明:
-- 1. 本脚本已移除所有外键约束（REFERENCES），改用应用层维护数据完整性
-- 2. 保留所有索引以确保查询性能
-- 3. 删除操作需要在应用层处理级联逻辑
-- 4. 优势：提高插入/更新性能，避免锁竞争，支持分库分表
-- ============================================================================

-- 启用 PostGIS 扩展（用于矢量标注的空间数据存储）
-- # CREATE EXTENSION IF NOT EXISTS postgis;

-- 启用 pg_trgm 扩展（用于模糊查询优化）
-- # CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================================================
-- 1. 项目管理表 (biz_project)
-- ============================================================================
CREATE TABLE biz_project (
    project_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    manager_id BIGINT,
    ethics_code VARCHAR(100),
    privacy_level SMALLINT DEFAULT 1,
    description TEXT,
    target_classes JSONB,
    status VARCHAR(20) DEFAULT 'active',
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    del_flag BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE biz_project IS '项目管理表';
COMMENT ON COLUMN biz_project.project_id IS '主键ID';
COMMENT ON COLUMN biz_project.name IS '项目名称';
COMMENT ON COLUMN biz_project.code IS '项目编码（唯一）';
COMMENT ON COLUMN biz_project.manager_id IS '负责人ID';
COMMENT ON COLUMN biz_project.ethics_code IS '伦理批件号';
COMMENT ON COLUMN biz_project.privacy_level IS '隐私级别 (1:公开, 2:脱敏, 3:绝密)';
COMMENT ON COLUMN biz_project.description IS '项目描述';
COMMENT ON COLUMN biz_project.target_classes IS '目标检测类别配置 (JSONB)';
COMMENT ON COLUMN biz_project.status IS '状态 (active/archived/deleted)';
COMMENT ON COLUMN biz_project.create_by IS '创建人ID';
COMMENT ON COLUMN biz_project.create_time IS '创建时间';
COMMENT ON COLUMN biz_project.update_by IS '更新人ID';
COMMENT ON COLUMN biz_project.update_time IS '更新时间';
COMMENT ON COLUMN biz_project.del_flag IS '删除标志';

-- 索引优化
CREATE INDEX idx_project_status ON biz_project(status);
CREATE INDEX idx_project_manager ON biz_project(manager_id);

-- ============================================================================
-- 2. 采集批次表 (biz_batch)
-- ============================================================================
CREATE TABLE biz_batch (
    batch_id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    batch_code VARCHAR(100) NOT NULL,
    batch_name VARCHAR(200),
    scanner_model VARCHAR(100),
    staining_protocol VARCHAR(100),
    storage_root_path VARCHAR(500),
    total_images INT DEFAULT 0,
    upload_status VARCHAR(20) DEFAULT 'pending',
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_batch IS '采集批次表';
COMMENT ON COLUMN biz_batch.batch_id IS '主键ID';
COMMENT ON COLUMN biz_batch.project_id IS '所属项目ID';
COMMENT ON COLUMN biz_batch.batch_code IS '批次编号';
COMMENT ON COLUMN biz_batch.batch_name IS '批次名称';
COMMENT ON COLUMN biz_batch.scanner_model IS '扫描仪型号';
COMMENT ON COLUMN biz_batch.staining_protocol IS '染色协议';
COMMENT ON COLUMN biz_batch.storage_root_path IS '原始存储根路径';
COMMENT ON COLUMN biz_batch.total_images IS '批次内图像总数';
COMMENT ON COLUMN biz_batch.upload_status IS '上传状态 (pending/uploading/completed/failed)';

-- 索引优化
CREATE INDEX idx_batch_project ON biz_batch(project_id);
CREATE INDEX idx_batch_code ON biz_batch(batch_code);

-- ============================================================================
-- 3. 切片表 (biz_slide) - 新增
-- ============================================================================
CREATE TABLE biz_slide (
    slide_id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    slide_code VARCHAR(100) NOT NULL UNIQUE,
    slide_name VARCHAR(200),
    pathology_id VARCHAR(100),
    patient_id VARCHAR(100),
    staining_type VARCHAR(50),
    tissue_type VARCHAR(100),
    diagnosis VARCHAR(500),
    clinical_info JSONB,
    storage_path VARCHAR(500),
    tile_storage_path VARCHAR(500),
    thumbnail_url VARCHAR(500),
    width INT,
    height INT,
    levels INT,
    mpp_x FLOAT,
    mpp_y FLOAT,
    magnification INT,
    file_size BIGINT,
    format VARCHAR(20),
    scanner_model VARCHAR(100),
    scan_date TIMESTAMP,
    quality_score FLOAT,
    qc_status VARCHAR(20) DEFAULT 'PENDING',
    qc_comment TEXT,
    qc_by BIGINT,
    qc_time TIMESTAMP,
    annotation_count INT DEFAULT 0,
    verified_annotation_count INT DEFAULT 0,
    lifecycle_status VARCHAR(20) DEFAULT 'Raw',
    is_public BOOLEAN DEFAULT FALSE,
    privacy_level SMALLINT DEFAULT 1,
    tags JSONB,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    del_flag BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE biz_slide IS '切片表（WSI完整切片信息）';
COMMENT ON COLUMN biz_slide.slide_id IS '主键ID（切片ID）';
COMMENT ON COLUMN biz_slide.image_id IS '关联图像ID';
COMMENT ON COLUMN biz_slide.project_id IS '所属项目ID（冗余字段，便于查询）';
COMMENT ON COLUMN biz_slide.batch_id IS '所属批次ID（冗余字段，便于查询）';
COMMENT ON COLUMN biz_slide.slide_code IS '切片编码（唯一标识）';
COMMENT ON COLUMN biz_slide.slide_name IS '切片名称';
COMMENT ON COLUMN biz_slide.pathology_id IS '病理号';
COMMENT ON COLUMN biz_slide.patient_id IS '患者ID（脱敏）';
COMMENT ON COLUMN biz_slide.staining_type IS '染色类型 (HE/IHC/IF等)';
COMMENT ON COLUMN biz_slide.tissue_type IS '组织类型';
COMMENT ON COLUMN biz_slide.diagnosis IS '诊断结果';
COMMENT ON COLUMN biz_slide.clinical_info IS '临床信息 (JSONB)';
COMMENT ON COLUMN biz_slide.storage_path IS '原始文件存储路径';
COMMENT ON COLUMN biz_slide.tile_storage_path IS '瓦片存储路径';
COMMENT ON COLUMN biz_slide.thumbnail_url IS '缩略图URL';
COMMENT ON COLUMN biz_slide.width IS '图像宽度（像素）';
COMMENT ON COLUMN biz_slide.height IS '图像高度（像素）';
COMMENT ON COLUMN biz_slide.levels IS '金字塔层级数';
COMMENT ON COLUMN biz_slide.mpp_x IS 'X轴物理分辨率 (um/px)';
COMMENT ON COLUMN biz_slide.mpp_y IS 'Y轴物理分辨率 (um/px)';
COMMENT ON COLUMN biz_slide.magnification IS '放大倍数';
COMMENT ON COLUMN biz_slide.file_size IS '文件大小（字节）';
COMMENT ON COLUMN biz_slide.format IS '格式 (SVS/NDPI/JPG/PNG)';
COMMENT ON COLUMN biz_slide.scanner_model IS '扫描仪型号';
COMMENT ON COLUMN biz_slide.scan_date IS '扫描日期';
COMMENT ON COLUMN biz_slide.quality_score IS '质量评分 (0-100)';
COMMENT ON COLUMN biz_slide.qc_status IS '质检状态 (PENDING/PASSED/FAILED)';
COMMENT ON COLUMN biz_slide.qc_comment IS '质检备注';
COMMENT ON COLUMN biz_slide.qc_by IS '质检人ID';
COMMENT ON COLUMN biz_slide.qc_time IS '质检时间';
COMMENT ON COLUMN biz_slide.annotation_count IS '标注总数';
COMMENT ON COLUMN biz_slide.verified_annotation_count IS '已审核标注数';
COMMENT ON COLUMN biz_slide.lifecycle_status IS '生命周期状态 (Raw/Processing/Annotated/Verified/Archived)';
COMMENT ON COLUMN biz_slide.is_public IS '是否公开';
COMMENT ON COLUMN biz_slide.privacy_level IS '隐私级别 (1:公开, 2:脱敏, 3:绝密)';
COMMENT ON COLUMN biz_slide.tags IS '标签集合 (JSONB)';

-- 索引优化
CREATE INDEX idx_slide_image ON biz_slide(image_id);
CREATE INDEX idx_slide_project ON biz_slide(project_id);
CREATE INDEX idx_slide_batch ON biz_slide(batch_id);
CREATE INDEX idx_slide_code ON biz_slide(slide_code);
CREATE INDEX idx_slide_pathology ON biz_slide(pathology_id);
CREATE INDEX idx_slide_patient ON biz_slide(patient_id);
CREATE INDEX idx_slide_lifecycle ON biz_slide(lifecycle_status);
CREATE INDEX idx_slide_qc_status ON biz_slide(qc_status);
CREATE INDEX idx_slide_create_time ON biz_slide(create_time DESC);
CREATE INDEX idx_slide_scan_date ON biz_slide(scan_date);

-- ============================================================================
-- 4. 图像表 (biz_image)
-- ============================================================================
CREATE TABLE biz_image (
    image_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_file_path VARCHAR(500),
    converted_tiff_path VARCHAR(500),
    pathology_id VARCHAR(100),
    patient_id VARCHAR(100),
    format VARCHAR(20),
    lifecycle_status VARCHAR(20) DEFAULT 'Raw',
    annotation_progress INT DEFAULT 0,
    levels INT,
    width INT,
    height INT,
    mpp_x FLOAT,
    mpp_y FLOAT,
    magnification INT,
    file_size BIGINT,
    scanner_info JSONB,
    metadata JSONB,
    thumbnail_url VARCHAR(500),
    requires_conversion BOOLEAN DEFAULT FALSE,
    conversion_status VARCHAR(20) DEFAULT 'NONE',
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    del_flag BOOLEAN DEFAULT FALSE
);

COMMENT ON TABLE biz_image IS '图像表';
COMMENT ON COLUMN biz_image.image_id IS '主键ID（图像ID）';
COMMENT ON COLUMN biz_image.batch_id IS '所属批次ID';
COMMENT ON COLUMN biz_image.filename IS '文件名';
COMMENT ON COLUMN biz_image.original_filename IS '原始文件名（用户上传时的文件名）';
COMMENT ON COLUMN biz_image.file_path IS '文件存储路径（兼容旧字段，逐步废弃）';
COMMENT ON COLUMN biz_image.original_file_path IS '原始文件路径（用户上传的原始文件）';
COMMENT ON COLUMN biz_image.converted_tiff_path IS '转换后 TIFF 文件路径（仅 JPG/PNG 需要，WSI 格式为空）';
COMMENT ON COLUMN biz_image.pathology_id IS '病理报告号';
COMMENT ON COLUMN biz_image.patient_id IS '患者ID（脱敏处理）';
COMMENT ON COLUMN biz_image.format IS '格式 (SVS/NDPI/JPG/PNG)';
COMMENT ON COLUMN biz_image.lifecycle_status IS '生命周期状态 (Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived)';
COMMENT ON COLUMN biz_image.annotation_progress IS '标注进度 (0-100)';
COMMENT ON COLUMN biz_image.levels IS '金字塔层级数（WSI图像的多分辨率层级数量）';
COMMENT ON COLUMN biz_image.width IS '图像宽度（像素）';
COMMENT ON COLUMN biz_image.height IS '图像高度（像素）';
COMMENT ON COLUMN biz_image.mpp_x IS 'X轴物理分辨率 (um/px)';
COMMENT ON COLUMN biz_image.mpp_y IS 'Y轴物理分辨率 (um/px)';
COMMENT ON COLUMN biz_image.magnification IS '放大倍数';
COMMENT ON COLUMN biz_image.file_size IS '文件大小（字节）';
COMMENT ON COLUMN biz_image.scanner_info IS '扫描仪详细信息 (JSONB)';
COMMENT ON COLUMN biz_image.metadata IS '扩展元数据 (JSONB)';
COMMENT ON COLUMN biz_image.thumbnail_url IS '缩略图URL';
COMMENT ON COLUMN biz_image.requires_conversion IS '是否需要转换（JPG/PNG 为 true，WSI 为 false）';
COMMENT ON COLUMN biz_image.conversion_status IS '转换状态 (NONE/PENDING/COMPLETED/FAILED)';

-- 索引优化
CREATE INDEX idx_image_batch ON biz_image(batch_id);
CREATE INDEX idx_image_search ON biz_image(batch_id, lifecycle_status);
CREATE INDEX idx_pathology_trgm ON biz_image USING gin(pathology_id gin_trgm_ops);
CREATE INDEX idx_patient_id ON biz_image(patient_id);
CREATE INDEX idx_lifecycle_status ON biz_image(lifecycle_status);

-- ============================================================================
-- 5. 标签定义表 (biz_tag)
-- ============================================================================
CREATE TABLE biz_tag (
    tag_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    category VARCHAR(50),
    parent_id BIGINT,
    color_code VARCHAR(20),
    sort_order INT DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_tag IS '标签定义表';
COMMENT ON COLUMN biz_tag.tag_id IS '主键ID';
COMMENT ON COLUMN biz_tag.name IS '标签名称';
COMMENT ON COLUMN biz_tag.code IS '标签编码（唯一）';
COMMENT ON COLUMN biz_tag.category IS '标签分类';
COMMENT ON COLUMN biz_tag.parent_id IS '父标签ID（实现层级结构）';
COMMENT ON COLUMN biz_tag.color_code IS '前端展示颜色';
COMMENT ON COLUMN biz_tag.sort_order IS '排序序号';
COMMENT ON COLUMN biz_tag.is_system IS '是否系统标签';

-- 索引优化
CREATE INDEX idx_tag_category ON biz_tag(category);
CREATE INDEX idx_tag_parent ON biz_tag(parent_id);

-- ============================================================================
-- 6. 图像标签关联表 (biz_image_tag_rel)
-- ============================================================================
CREATE TABLE biz_image_tag_rel (
    rel_id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    confidence FLOAT,
    tagged_by BIGINT,
    tag_source VARCHAR(20),
    vector_annotation_id BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_image_tag_rel IS '图像标签关联表';
COMMENT ON COLUMN biz_image_tag_rel.rel_id IS '主键ID';
COMMENT ON COLUMN biz_image_tag_rel.image_id IS '图像ID';
COMMENT ON COLUMN biz_image_tag_rel.tag_id IS '标签ID';
COMMENT ON COLUMN biz_image_tag_rel.confidence IS '置信度 (0-1)';
COMMENT ON COLUMN biz_image_tag_rel.tagged_by IS '打标人ID';
COMMENT ON COLUMN biz_image_tag_rel.tag_source IS '标签来源 (AI_PRE_ANNOTATION/MANUAL/SYSTEM_AUTO)';
COMMENT ON COLUMN biz_image_tag_rel.vector_annotation_id IS '关联矢量标注ID（可选）';

-- 索引优化
CREATE UNIQUE INDEX idx_rel_unique ON biz_image_tag_rel(image_id, tag_id, tagged_by) 
    WHERE tagged_by IS NOT NULL;
CREATE INDEX idx_rel_tag ON biz_image_tag_rel(tag_id, image_id);
CREATE INDEX idx_rel_image ON biz_image_tag_rel(image_id, tag_id);
CREATE INDEX idx_rel_vector ON biz_image_tag_rel(vector_annotation_id) 
    WHERE vector_annotation_id IS NOT NULL;

-- ============================================================================
-- 7. 任务执行表 (biz_task)
-- ============================================================================
CREATE TABLE biz_task (
    task_id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    project_id BIGINT,
    model_version VARCHAR(50),
    config_snapshot JSONB,
    progress FLOAT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    result_summary JSONB,
    error_message TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INT,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_task IS '任务执行表';
COMMENT ON COLUMN biz_task.task_id IS '主键ID';
COMMENT ON COLUMN biz_task.task_no IS '任务编号（唯一）';
COMMENT ON COLUMN biz_task.type IS '任务类型 (TRAINING/PREDICTION/PRE_ANNOTATION)';
COMMENT ON COLUMN biz_task.project_id IS '所属项目ID';
COMMENT ON COLUMN biz_task.model_version IS '关联的模型版本';
COMMENT ON COLUMN biz_task.config_snapshot IS '任务配置快照 (JSONB)';
COMMENT ON COLUMN biz_task.progress IS '当前进度 (0-100)';
COMMENT ON COLUMN biz_task.status IS '状态 (PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)';
COMMENT ON COLUMN biz_task.result_summary IS '结果摘要 (JSONB)';
COMMENT ON COLUMN biz_task.error_message IS '错误信息';
COMMENT ON COLUMN biz_task.start_time IS '开始时间';
COMMENT ON COLUMN biz_task.end_time IS '结束时间';
COMMENT ON COLUMN biz_task.duration_seconds IS '耗时（秒）';

-- 索引优化
CREATE INDEX idx_task_project ON biz_task(project_id, type);
CREATE INDEX idx_task_status ON biz_task(status);

-- ============================================================================
-- 8. 模型注册表 (biz_model)
-- ============================================================================
CREATE TABLE biz_model (
    model_id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    base_model VARCHAR(50),
    project_id BIGINT,
    training_task_id BIGINT,
    metrics JSONB,
    weights_path VARCHAR(500),
    is_best BOOLEAN DEFAULT FALSE,
    dataset_snapshot JSONB,
    hyperparams JSONB,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_model IS '模型注册表';
COMMENT ON COLUMN biz_model.model_id IS '主键ID';
COMMENT ON COLUMN biz_model.model_name IS '模型名称';
COMMENT ON COLUMN biz_model.version IS '版本号';
COMMENT ON COLUMN biz_model.base_model IS '基座模型';
COMMENT ON COLUMN biz_model.project_id IS '所属项目ID';
COMMENT ON COLUMN biz_model.training_task_id IS '关联的训练任务ID';
COMMENT ON COLUMN biz_model.metrics IS '性能指标 (JSONB)';
COMMENT ON COLUMN biz_model.weights_path IS '权重文件路径';
COMMENT ON COLUMN biz_model.is_best IS '是否为当前最优模型';
COMMENT ON COLUMN biz_model.dataset_snapshot IS '训练数据集快照 (JSONB)';
COMMENT ON COLUMN biz_model.hyperparams IS '超参数配置 (JSONB)';

-- 索引优化
CREATE INDEX idx_model_project ON biz_model(project_id);
CREATE UNIQUE INDEX idx_model_version ON biz_model(model_name, version);

-- ============================================================================
-- 9. 预测结果表 (biz_prediction)
-- ============================================================================
CREATE TABLE biz_prediction (
    prediction_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    detections JSONB,
    geojson_path VARCHAR(500),
    overlay_img_path VARCHAR(500),
    object_count INT DEFAULT 0,
    positive_rate FLOAT,
    inference_time_ms INT,
    review_status VARCHAR(20) DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_comment TEXT,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_prediction IS '预测结果表';
COMMENT ON COLUMN biz_prediction.prediction_id IS '主键ID';
COMMENT ON COLUMN biz_prediction.task_id IS '关联预测任务ID';
COMMENT ON COLUMN biz_prediction.image_id IS '关联图像ID';
COMMENT ON COLUMN biz_prediction.detections IS '检测结果数组 (JSONB)';
COMMENT ON COLUMN biz_prediction.geojson_path IS 'GeoJSON文件路径';
COMMENT ON COLUMN biz_prediction.overlay_img_path IS '叠加标注框的可视化图像路径';
COMMENT ON COLUMN biz_prediction.object_count IS '检出目标数量';
COMMENT ON COLUMN biz_prediction.positive_rate IS '阳性率';
COMMENT ON COLUMN biz_prediction.inference_time_ms IS '推理耗时（毫秒）';
COMMENT ON COLUMN biz_prediction.review_status IS '质检状态 (PENDING/APPROVED/REJECTED)';
COMMENT ON COLUMN biz_prediction.reviewer_id IS '质检员ID';
COMMENT ON COLUMN biz_prediction.review_comment IS '质检备注';

-- 索引优化
CREATE INDEX idx_pred_task ON biz_prediction(task_id);
CREATE INDEX idx_pred_image ON biz_prediction(image_id);
CREATE INDEX idx_review_status ON biz_prediction(review_status);

-- ============================================================================
-- 10. 矢量标注表 (biz_annotation)
-- ============================================================================
CREATE TABLE biz_annotation (
    annotation_id BIGSERIAL PRIMARY KEY,
    slide_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    project_id BIGINT,
    batch_id BIGINT,
    tag_id BIGINT NOT NULL,
    parent_annotation_id BIGINT,
    geom_type VARCHAR(20) NOT NULL,
    
    -- 空间数据存储（二选一，推荐 geom）
    geom GEOMETRY(Geometry, 0),  -- PostGIS几何字段（主存储，支持空间索引和查询）
--     coordinates_geojson JSONB,   -- GeoJSON备份（可选，用于前端快速渲染，可为NULL）
    
    -- LOD多分辨率支持
    lod_level INT DEFAULT 0,     -- LOD层级 (0=原始精度, 1-5=简化层级，数字越大越简化)
    simplified_geom GEOMETRY(Geometry, 0),  -- 简化后的几何（用于低分辨率快速渲染）
    bbox GEOMETRY(Geometry, 0),  -- 边界框（用于快速筛选、视口裁剪和碰撞检测）
    
    -- 标注属性
    confidence FLOAT,
    area NUMERIC(38,2),
    perimeter NUMERIC(38,2),
    centroid_x FLOAT,
    centroid_y FLOAT,
    description TEXT,
    created_by BIGINT,
    creation_source VARCHAR(20),
    review_status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_time TIMESTAMP,
    version INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_annotation IS '矢量标注表';
COMMENT ON COLUMN biz_annotation.annotation_id IS '主键ID（标注对象ID）';
COMMENT ON COLUMN biz_annotation.slide_id IS '关联切片ID（主要查询维度）';
COMMENT ON COLUMN biz_annotation.image_id IS '关联图像ID（冗余字段）';
COMMENT ON COLUMN biz_annotation.project_id IS '所属项目ID（冗余字段，便于按项目统计）';
COMMENT ON COLUMN biz_annotation.batch_id IS '所属批次ID（冗余字段，便于按批次统计）';
COMMENT ON COLUMN biz_annotation.tag_id IS '关联标签ID';
COMMENT ON COLUMN biz_annotation.parent_annotation_id IS '父标注ID（支持层级标注，如：脏器->组织）';
COMMENT ON COLUMN biz_annotation.geom_type IS '标注类型 (POINT/LINESTRING/POLYGON/MULTIPOLYGON)';
COMMENT ON COLUMN biz_annotation.geom IS 'PostGIS几何字段（主存储，支持空间索引和精确查询）';
-- COMMENT ON COLUMN biz_annotation.coordinates_geojson IS 'GeoJSON格式坐标（可选备份，用于前端快速渲染，可为NULL避免冗余）';
COMMENT ON COLUMN biz_annotation.lod_level IS 'LOD层级 (0=原始精度, 1-5=简化层级，数字越大越简化，用于多分辨率渲染)';
COMMENT ON COLUMN biz_annotation.simplified_geom IS '简化后的几何（用于低分辨率快速渲染，减少数据传输量）';
COMMENT ON COLUMN biz_annotation.bbox IS '边界框（用于快速筛选、视口裁剪和碰撞检测）';
COMMENT ON COLUMN biz_annotation.confidence IS '置信度 (0-1)';
COMMENT ON COLUMN biz_annotation.area IS '标注区域面积（微米²）';
COMMENT ON COLUMN biz_annotation.perimeter IS '周长（微米）';
COMMENT ON COLUMN biz_annotation.centroid_x IS '质心X坐标';
COMMENT ON COLUMN biz_annotation.centroid_y IS '质心Y坐标';
COMMENT ON COLUMN biz_annotation.description IS '标注描述信息';
COMMENT ON COLUMN biz_annotation.created_by IS '创建人ID';
COMMENT ON COLUMN biz_annotation.creation_source IS '来源 (AI_PRE_ANNOTATION/MANUAL_DRAWING/AUTO_SEGMENTATION)';
COMMENT ON COLUMN biz_annotation.review_status IS '审核状态 (PENDING/APPROVED/REJECTED/MODIFIED)';
COMMENT ON COLUMN biz_annotation.reviewed_by IS '审核人ID';
COMMENT ON COLUMN biz_annotation.review_time IS '审核时间';
COMMENT ON COLUMN biz_annotation.version IS '版本号（支持修改历史）';
COMMENT ON COLUMN biz_annotation.is_active IS '是否有效';
COMMENT ON COLUMN biz_annotation.sort_order IS '排序序号（同一层级内的显示顺序）';

-- 索引优化
CREATE INDEX idx_vec_spatial ON biz_annotation USING GIST(geom);
CREATE INDEX idx_vec_slide ON biz_annotation(slide_id, is_active);
CREATE INDEX idx_vec_image ON biz_annotation(image_id, is_active);
CREATE INDEX idx_vec_project ON biz_annotation(project_id, is_active) WHERE project_id IS NOT NULL;
CREATE INDEX idx_vec_batch ON biz_annotation(batch_id, is_active) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_vec_tag ON biz_annotation(tag_id, is_active);
CREATE INDEX idx_vec_parent ON biz_annotation(parent_annotation_id) WHERE parent_annotation_id IS NOT NULL;
CREATE INDEX idx_vec_review ON biz_annotation(review_status, slide_id) 
    WHERE review_status = 'PENDING';
CREATE INDEX idx_vec_slide_tag ON biz_annotation(slide_id, tag_id, is_active);
CREATE INDEX idx_vec_image_tag ON biz_annotation(image_id, tag_id, is_active);
CREATE INDEX idx_vec_create_time ON biz_annotation(create_time DESC);
CREATE INDEX idx_vec_update_time ON biz_annotation(update_time DESC);
CREATE INDEX idx_vec_description ON biz_annotation(description);

-- LOD多分辨率索引
CREATE INDEX idx_vec_lod ON biz_annotation(slide_id, lod_level, is_active);
CREATE INDEX idx_vec_bbox ON biz_annotation USING GIST(bbox) WHERE bbox IS NOT NULL;

-- ============================================================================
-- 初始化示例数据（可选）
-- ============================================================================

-- 插入示例标签
INSERT INTO biz_tag (name, code, category, parent_id, color_code, sort_order, is_system) VALUES
('器官', 'ORGAN_ROOT', 'category', NULL, '#000000', 1, TRUE),
('肺', 'ORGAN_LUNG', 'organ', 1, '#FF5733', 1, FALSE),
('肝', 'ORGAN_LIVER', 'organ', 1, '#33FF57', 2, FALSE),
('肺腺癌', 'LUNG_ADENOCARCINOMA', 'disease', 2, '#FF33F5', 1, FALSE),
('肺鳞癌', 'LUNG_SQUAMOUS_CELL', 'disease', 2, '#3357FF', 2, FALSE),
('肝癌', 'LIVER_CANCER', 'disease', 3, '#F5FF33', 1, FALSE),
('病变', 'LESION_ROOT', 'category', NULL, '#000000', 2, TRUE),
('PD-L1阳性', 'PD_L1_POSITIVE', 'indicator', 7, '#FF8C33', 1, FALSE),
('Ki-67高表达', 'KI67_HIGH', 'indicator', 7, '#8C33FF', 2, FALSE);

-- ============================================================================
-- 层级标注查询示例
-- ============================================================================

-- 场景1: 查询某脏器（如“肺”）下的所有组织标注
-- 假设：肺的标注ID为100，需要查询其下所有子标注
WITH RECURSIVE annotation_tree AS (
    -- 锚点：找到父标注（脏器）
    SELECT 
        ann.annotation_id,
        ann.image_id,
        ann.tag_id,
        ann.parent_annotation_id,
        ann.geom_type,
        ann.geom,
        t.name as tag_name,
        t.category as tag_category,
        1 as level
    FROM biz_annotation ann
    JOIN biz_tag t ON ann.tag_id = t.tag_id
    WHERE ann.annotation_id = 100  -- 脏器标注ID
      AND ann.is_active = true
    
    UNION ALL
    
    -- 递归：查找所有子标注（组织）
    SELECT 
        child.annotation_id,
        child.image_id,
        child.tag_id,
        child.parent_annotation_id,
        child.geom_type,
        child.geom,
        t.name as tag_name,
        t.category as tag_category,
        parent.level + 1 as level
    FROM biz_annotation child
    JOIN annotation_tree parent ON child.parent_annotation_id = parent.annotation_id
    JOIN biz_tag t ON child.tag_id = t.tag_id
    WHERE child.is_active = true
)
SELECT * FROM annotation_tree
ORDER BY level, annotation_id;

-- 场景2: 查询某图像下所有顶级标注（无父标注的脏器）
SELECT 
    ann.annotation_id,
    ann.tag_id,
    t.name as tag_name,
    t.category,
    ann.geom_type,
    ST_AsText(ann.geom) as geometry_wkt,
    ann.area_pixels,
    ann.confidence
FROM biz_annotation ann
JOIN biz_tag t ON ann.tag_id = t.tag_id
WHERE ann.image_id = 456
  AND ann.parent_annotation_id IS NULL  -- 顶级标注
  AND ann.is_active = true
ORDER BY ann.sort_order, ann.create_time;

-- 场景3: 统计某脏器下的组织数量和类型分布
SELECT 
    parent.tag_id as organ_tag_id,
    parent_tag.name as organ_name,
    COUNT(child.annotation_id) as tissue_count,
    STRING_AGG(DISTINCT child_tag.name, ', ') as tissue_types
FROM biz_annotation parent
JOIN biz_tag parent_tag ON parent.tag_id = parent_tag.tag_id
LEFT JOIN biz_annotation child ON child.parent_annotation_id = parent.annotation_id
LEFT JOIN biz_tag child_tag ON child.tag_id = child_tag.tag_id
WHERE parent.image_id = 456
  AND parent.parent_annotation_id IS NULL  -- 顶级脏器
  AND parent.is_active = true
GROUP BY parent.tag_id, parent_tag.name
ORDER BY tissue_count DESC;

-- 场景4: 空间查询 - 查找某区域内的所有标注及其层级关系
WITH target_region AS (
    SELECT ST_GeomFromText('POLYGON((100 100, 500 100, 500 500, 100 500, 100 100))', 0) as geom
)
SELECT 
    ann.annotation_id,
    ann.parent_annotation_id,
    ann.tag_id,
    t.name as tag_name,
    ann.geom_type,
    ST_AsText(ann.geom) as geometry_wkt,
    CASE 
        WHEN ann.parent_annotation_id IS NULL THEN '脏器'
        ELSE '组织'
    END as annotation_level,
    ST_Area(ann.geom) as area_pixels
FROM biz_annotation ann
JOIN biz_tag t ON ann.tag_id = t.tag_id
CROSS JOIN target_region tr
WHERE ann.image_id = 456
  AND ann.is_active = true
  AND ST_Intersects(ann.geom, tr.geom)  -- 空间相交
ORDER BY ann.parent_annotation_id NULLS FIRST, ann.sort_order;

-- 场景5: 获取完整的标注树结构（JSON格式）
SELECT 
    ann.annotation_id,
    ann.tag_id,
    t.name as tag_name,
    ann.parent_annotation_id,
    ann.geom_type,
    json_build_object(
        'id', ann.annotation_id,
        'tagId', ann.tag_id,
        'tagName', t.name,
        'type', ann.geom_type,
        'geometry', ST_AsGeoJSON(ann.geom)::json,
        'children', COALESCE(children.data, '[]'::json)
    ) as annotation_node
FROM biz_annotation ann
JOIN biz_tag t ON ann.tag_id = t.tag_id
LEFT JOIN LATERAL (
    SELECT json_agg(
        json_build_object(
            'id', child.annotation_id,
            'tagId', child.tag_id,
            'tagName', child_tag.name,
            'type', child.geom_type,
            'geometry', ST_AsGeoJSON(child.geom)::json
        )
    ) as data
    FROM biz_annotation child
    JOIN biz_tag child_tag ON child.tag_id = child_tag.tag_id
    WHERE child.parent_annotation_id = ann.annotation_id
      AND child.is_active = true
) children ON true
WHERE ann.image_id = 456
  AND ann.parent_annotation_id IS NULL  -- 只返回顶级节点
  AND ann.is_active = true
ORDER BY ann.sort_order;

-- ============================================================================
-- LOD 多分辨率渲染优化
-- ============================================================================

-- 场景1: 根据缩放级别自动选择 LOD 层级
-- 前端缩放级别与 LOD 映射关系：
--   zoom < 5:  lod_level = 5 (最简化)
--   zoom 5-8:  lod_level = 4
--   zoom 8-11: lod_level = 3
--   zoom 11-14: lod_level = 2
--   zoom 14-17: lod_level = 1
--   zoom >= 17: lod_level = 0 (原始精度)

CREATE OR REPLACE FUNCTION get_annotations_by_zoom(
    p_slide_id BIGINT,
    p_zoom_level INT,
    p_min_x FLOAT DEFAULT NULL,
    p_min_y FLOAT DEFAULT NULL,
    p_max_x FLOAT DEFAULT NULL,
    p_max_y FLOAT DEFAULT NULL
) RETURNS TABLE (
    annotation_id BIGINT,
    tag_id BIGINT,
    tag_name VARCHAR(100),
    parent_annotation_id BIGINT,
    geom_type VARCHAR(20),
    geom GEOMETRY,
    lod_level INT,
    area_pixels BIGINT,
    confidence FLOAT
) AS $$
DECLARE
    v_lod_level INT;
BEGIN
    -- 根据缩放级别计算 LOD 层级
    v_lod_level := CASE 
        WHEN p_zoom_level < 5 THEN 5
        WHEN p_zoom_level < 8 THEN 4
        WHEN p_zoom_level < 11 THEN 3
        WHEN p_zoom_level < 14 THEN 2
        WHEN p_zoom_level < 17 THEN 1
        ELSE 0
    END;
    
    RETURN QUERY
    SELECT 
        ann.annotation_id,
        ann.tag_id,
        t.name as tag_name,
        ann.parent_annotation_id,
        ann.geom_type,
        COALESCE(ann.simplified_geom, ann.geom) as geom,  -- 优先使用简化几何
        ann.lod_level,
        ann.area_pixels,
        ann.confidence
    FROM biz_annotation ann
    JOIN biz_tag t ON ann.tag_id = t.tag_id
    WHERE ann.slide_id = p_slide_id  -- 使用 slide_id
      AND ann.is_active = true
      AND ann.lod_level <= v_lod_level  -- 返回当前层级及更简化的数据
      AND (
          -- 视口裁剪（可选）
          p_min_x IS NULL OR 
          ST_Intersects(ann.bbox, ST_MakeEnvelope(p_min_x, p_min_y, p_max_x, p_max_y, 0))
      )
    ORDER BY ann.lod_level ASC, ann.area_pixels DESC;  -- 先显示大目标
END;
$$ LANGUAGE plpgsql;

-- 使用示例：
-- SELECT * FROM get_annotations_by_zoom(789, 10);  -- slide_id=789, zoom=10
-- SELECT * FROM get_annotations_by_zoom(789, 18, 100, 100, 500, 500);  -- 带视口裁剪


-- 场景2: 自动生成 LOD 简化几何（批量处理）
CREATE OR REPLACE FUNCTION generate_lod_geometries(
    p_slide_id BIGINT,
    p_max_lod INT DEFAULT 5
) RETURNS VOID AS $$
DECLARE
    rec RECORD;
    v_simplified GEOMETRY;
    v_bbox GEOMETRY;
BEGIN
    -- 遍历所有标注，生成简化几何和边界框
    FOR rec IN 
        SELECT annotation_id, geom 
        FROM biz_annotation 
        WHERE slide_id = p_slide_id  -- 使用 slide_id
          AND is_active = true
          AND geom IS NOT NULL
          AND lod_level = 0  -- 只处理原始精度标注
    LOOP
        -- 生成边界框
        v_bbox := ST_Envelope(rec.geom);
        
        -- 更新原始标注的 bbox 和 lod_level
        UPDATE biz_annotation
        SET bbox = v_bbox,
            lod_level = 0
        WHERE annotation_id = rec.annotation_id;
        
        -- 生成不同 LOD 层级的简化几何（作为独立记录）
        FOR i IN 1..p_max_lod LOOP
            -- 使用 Douglas-Peucker 算法简化
            v_simplified := ST_SimplifyPreserveTopology(
                rec.geom, 
                10 * POWER(2, i)  -- 容差随 LOD 增加而增大
            );
            
            -- 插入简化版本
            INSERT INTO biz_annotation (
                slide_id, image_id, project_id, batch_id, tag_id, parent_annotation_id, geom_type,
                geom, simplified_geom, bbox, lod_level,
                area_pixels, perimeter, centroid_x, centroid_y,
                created_by, creation_source, is_active, create_time, update_time
            )
            SELECT 
                slide_id, image_id, project_id, batch_id, tag_id, parent_annotation_id, geom_type,
                rec.geom, v_simplified, v_bbox, i,
                ST_Area(rec.geom), ST_Perimeter(rec.geom),
                ST_X(ST_Centroid(rec.geom)), ST_Y(ST_Centroid(rec.geom)),
                created_by, 'AUTO_LOD_GENERATION', true, NOW(), NOW()
            FROM biz_annotation
            WHERE annotation_id = rec.annotation_id;
        END LOOP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 使用示例：
-- SELECT generate_lod_geometries(789, 5);  -- 为切片789生成5个LOD层级


-- ============================================================================
-- 增量加载优化（Progressive Loading）
-- ============================================================================

-- 按优先级分批加载标注，先显示重要/大的标注，再逐步加载细节

CREATE OR REPLACE FUNCTION get_annotations_progressive(
    p_slide_id BIGINT,
    p_batch_size INT DEFAULT 100,
    p_offset INT DEFAULT 0,
    p_min_area FLOAT DEFAULT 0  -- 最小面积过滤（可选）
) RETURNS TABLE (
    annotation_id BIGINT,
    tag_id BIGINT,
    tag_name VARCHAR(100),
    parent_annotation_id BIGINT,
    geom GEOMETRY,
    priority INT,
    area_pixels BIGINT,
    confidence FLOAT,
    total_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH filtered_annotations AS (
        SELECT 
            ann.annotation_id,
            ann.tag_id,
            t.name as tag_name,
            ann.parent_annotation_id,
            COALESCE(ann.simplified_geom, ann.geom) as geom,
            CASE 
                WHEN ann.area_pixels > 100000 THEN 1  -- 优先级1：超大目标
                WHEN ann.area_pixels > 10000 THEN 2   -- 优先级2：大目标
                WHEN ann.area_pixels > 1000 THEN 3    -- 优先级3：中等目标
                ELSE 4                                 -- 优先级4：小目标
            END as priority,
            ann.area_pixels,
            ann.confidence,
            COUNT(*) OVER() as total_count
        FROM biz_annotation ann
        JOIN biz_tag t ON ann.tag_id = t.tag_id
        WHERE ann.slide_id = p_slide_id  -- 使用 slide_id
          AND ann.is_active = true
          AND ann.lod_level <= 2  -- 只加载中低精度
          AND (p_min_area = 0 OR ann.area_pixels >= p_min_area)
    )
    SELECT 
        fa.annotation_id,
        fa.tag_id,
        fa.tag_name,
        fa.parent_annotation_id,
        fa.geom,
        fa.priority,
        fa.area_pixels,
        fa.confidence,
        fa.total_count
    FROM filtered_annotations fa
    ORDER BY fa.priority ASC, fa.area_pixels DESC
    LIMIT p_batch_size
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- 使用示例：分3批加载
-- 第1批：SELECT * FROM get_annotations_progressive(789, 100, 0);       -- 最重要的100个
-- 第2批：SELECT * FROM get_annotations_progressive(789, 100, 100);     -- 次要的100个
-- 第3批：SELECT * FROM get_annotations_progressive(789, 100, 200);     -- 其余
-- 过滤小目标：SELECT * FROM get_annotations_progressive(789, 100, 0, 1000);  -- 只显示面积>1000的


-- ============================================================================
-- MVT 矢量瓦片优化（Mapbox Vector Tiles）
-- ============================================================================

-- 生成 MVT 格式数据，前端使用 GPU 直接渲染，适合超大规模数据

CREATE OR REPLACE FUNCTION get_annotations_as_mvt(
    p_slide_id BIGINT,
    p_zoom INT,
    p_x INT,
    p_y INT,
    p_extent INT DEFAULT 4096,
    p_lod_max INT DEFAULT 2
) RETURNS BYTEA AS $$
DECLARE
    v_mvt BYTEA;
    v_bounds GEOMETRY;
BEGIN
    -- 计算瓦片边界（Web Mercator投影）
    v_bounds := ST_TileEnvelope(p_zoom, p_x, p_y);
    
    -- 生成 MVT（需要安装 postgis-vt-util 扩展）
    SELECT ST_AsMVT(q, 'annotations', p_extent, 'geom')
    INTO v_mvt
    FROM (
        SELECT 
            annotation_id,
            tag_id,
            parent_annotation_id,
            area_pixels,
            confidence,
            ST_AsMVTGeom(
                COALESCE(simplified_geom, geom),  -- 优先使用简化几何
                v_bounds,
                p_extent,
                0,  -- buffer
                false  -- 不裁剪
            ) as geom
        FROM biz_annotation
        WHERE slide_id = p_slide_id  -- 使用 slide_id
          AND is_active = true
          AND lod_level <= p_lod_max
          AND ST_Intersects(bbox, v_bounds)  -- 使用边界框快速筛选
    ) q;
    
    RETURN v_mvt;
END;
$$ LANGUAGE plpgsql;

-- 使用示例：
-- SELECT get_annotations_as_mvt(789, 10, 512, 256);  -- slide_id=789, zoom=10
-- SELECT get_annotations_as_mvt(789, 12, 2048, 1024, 4096, 1);  -- 更高精度


-- MVT 瓦片统计信息
CREATE OR REPLACE FUNCTION get_mvt_tile_stats(
    p_slide_id BIGINT,
    p_zoom INT
) RETURNS TABLE (
    tile_x INT,
    tile_y INT,
    annotation_count BIGINT,
    avg_area FLOAT,
    has_simplified BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        FLOOR(ST_XMin(bbox)::INT / 256) as tile_x,
        FLOOR(ST_YMin(bbox)::INT / 256) as tile_y,
        COUNT(*) as annotation_count,
        AVG(area_pixels) as avg_area,
        BOOL_OR(simplified_geom IS NOT NULL) as has_simplified
    FROM biz_annotation
    WHERE slide_id = p_slide_id  -- 使用 slide_id
      AND is_active = true
      AND lod_level <= 2
      AND bbox IS NOT NULL
    GROUP BY tile_x, tile_y
    ORDER BY annotation_count DESC;
END;
$$ LANGUAGE plpgsql;

-- 使用示例：查看哪些瓦片数据量大
-- SELECT * FROM get_mvt_tile_stats(789, 10);

-- ============================================================================
-- 图像生命周期状态说明
-- ============================================================================

-- 状态定义:
-- 1. Raw (原始态): 刚上传，仅包含图像文件，未解析元数据
-- 2. Indexed (索引态): 元数据解析完成，标签已挂载，可检索
-- 3. Processing (处理中): 正在进行自动预标注、格式转换或LOD生成
-- 4. Annotated (已标注): 包含人工或AI生成的标注文件，待审核
-- 5. Verified (已审核): 专家复核通过，进入"金标准库"，具备训练资格
-- 6. Predicted (已预测): 已被模型推理过，生成了初步诊断建议
-- 7. Archived (归档态): 项目结束，数据冷存储

-- 状态流转图:
-- [*] --> Raw: 文件上传
-- Raw --> Indexed: 元数据解析成功
-- Indexed --> Processing: 触发预处理/预标注
-- Processing --> Annotated: AI生成初始标注
-- Annotated --> Verified: 人工质检通过
-- Verified --> Training_Queue: 加入训练数据集
-- Verified --> Predicted: 调用模型推理
-- Predicted --> Archived: 项目结项
-- Annotated --> Processing: 质检不通过(返工)

-- 状态查询示例:


-- ============================================================================
-- 数据集构建任务表 (biz_dataset_build_task) - 通用，支持多种算法
-- ============================================================================
CREATE TABLE biz_dataset_build_task (
    task_id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL,
    batch_ids JSONB,                    -- 批次ID列表（JSON数组，可选）
    tag_ids JSONB,                      -- 标签ID列表（JSON数组，可选）
    algorithm_type VARCHAR(50) NOT NULL DEFAULT 'YOLO', -- 算法类型（YOLO, RCNN, SSD等）
    task_name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- ========== 数据集配置 ==========
    train_ratio FLOAT DEFAULT 0.7,      -- 训练集比例
    val_ratio FLOAT DEFAULT 0.2,        -- 验证集比例
    test_ratio FLOAT DEFAULT 0.1,       -- 测试集比例
    class_mapping JSONB,                -- 类别映射配置 {old_name: new_name}
    shuffle BOOLEAN DEFAULT TRUE,       -- 是否打乱数据
    
    -- ========== 输出配置 ==========
    output_format VARCHAR(20) DEFAULT 'yolov8', -- 输出格式 (yolov5/yolov8/coco等)
    include_images BOOLEAN DEFAULT TRUE,         -- 是否包含图像文件
    compress_format VARCHAR(10) DEFAULT 'none',  -- 压缩格式 (zip/tar.gz/none)
    compress_quality INT,                        -- 压缩质量（1-100）
    min_image_size INT,                          -- 图像最小尺寸过滤
    max_image_size INT,                          -- 图像最大尺寸过滤
    extra_config JSONB,                          -- 额外配置（JSON，不同算法可有不同配置）
    
    -- ========== 任务状态 ==========
    status VARCHAR(20) DEFAULT 'PENDING',        -- PENDING/RUNNING/SUCCESS/FAILED/CANCELLED
    progress FLOAT DEFAULT 0,                    -- 进度 0-100
    current_step VARCHAR(100),                   -- 当前执行步骤描述
    step_detail JSONB,                           -- 步骤详细信息
    
    -- ========== 结果信息 ==========
    total_images INT DEFAULT 0,                  -- 总图像数
    total_annotations INT DEFAULT 0,             -- 总标注数
    train_count INT DEFAULT 0,                   -- 训练集数量
    val_count INT DEFAULT 0,                     -- 验证集数量
    test_count INT DEFAULT 0,                    -- 测试集数量
    class_distribution JSONB,                    -- 类别分布统计 {class_name: count}
    dataset_path VARCHAR(500),                   -- 数据集文件路径
    dataset_size BIGINT,                         -- 数据集文件大小（字节）
    data_yaml_path VARCHAR(500),                 -- data.yaml配置文件路径
    
    -- ========== 错误信息 ==========
    error_message TEXT,
    error_stack TEXT,
    
    -- ========== 审计字段 ==========
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INT,
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_dataset_build_task IS '数据集构建任务表（通用，支持多种算法）';
COMMENT ON COLUMN biz_dataset_build_task.task_id IS '主键ID';
COMMENT ON COLUMN biz_dataset_build_task.task_no IS '任务编号（唯一）';
COMMENT ON COLUMN biz_dataset_build_task.project_id IS '所属项目ID';
COMMENT ON COLUMN biz_dataset_build_task.batch_ids IS '批次ID列表（JSON数组）';
COMMENT ON COLUMN biz_dataset_build_task.tag_ids IS '标签ID列表（JSON数组）';
COMMENT ON COLUMN biz_dataset_build_task.algorithm_type IS '算法类型：YOLO, RCNN, SSD等';
COMMENT ON COLUMN biz_dataset_build_task.train_ratio IS '训练集比例';
COMMENT ON COLUMN biz_dataset_build_task.val_ratio IS '验证集比例';
COMMENT ON COLUMN biz_dataset_build_task.test_ratio IS '测试集比例';
COMMENT ON COLUMN biz_dataset_build_task.class_mapping IS '类别映射配置（JSON对象）';
COMMENT ON COLUMN biz_dataset_build_task.output_format IS '输出格式（yolov5/yolov8/coco等）';
COMMENT ON COLUMN biz_dataset_build_task.status IS '任务状态';
COMMENT ON COLUMN biz_dataset_build_task.progress IS '任务进度（0-100）';
COMMENT ON COLUMN biz_dataset_build_task.current_step IS '当前执行步骤';
COMMENT ON COLUMN biz_dataset_build_task.step_detail IS '步骤详细信息（JSON）';
COMMENT ON COLUMN biz_dataset_build_task.dataset_path IS '生成的数据集文件路径';
COMMENT ON COLUMN biz_dataset_build_task.extra_config IS '额外配置（JSON，不同算法可有不同配置）';

-- 索引优化
CREATE INDEX idx_dataset_build_project ON biz_dataset_build_task(project_id);
CREATE INDEX idx_dataset_build_status ON biz_dataset_build_task(status);
CREATE INDEX idx_dataset_build_algorithm ON biz_dataset_build_task(algorithm_type);
CREATE INDEX idx_dataset_build_create_time ON biz_dataset_build_task(create_time DESC);
CREATE INDEX idx_dataset_build_task_no ON biz_dataset_build_task(task_no);

-- ============================================================================
-- YOLO模型训练任务表 (biz_yolo_training_task)
-- ============================================================================
CREATE TABLE biz_yolo_training_task (
    task_id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- ========== 数据源 ==========
    dataset_task_id BIGINT,                      -- 关联的数据集构建任务ID
    dataset_path VARCHAR(500),                   -- 数据集路径
    custom_dataset_path VARCHAR(500),            -- 自定义数据集路径
    dataset_config JSONB,                        -- 数据集配置快照
    
    -- ========== 训练配置 ==========
    model_architecture VARCHAR(50) DEFAULT 'yolov8n', -- 模型架构 (yolov8n/s/m/l/x)
    pretrained_weights VARCHAR(100),             -- 预训练权重 (coco/imagenet/custom)
    epochs INT DEFAULT 100,                      -- 训练轮数
    batch_size INT DEFAULT 16,                   -- 批次大小
    image_size INT DEFAULT 640,                  -- 图像尺寸
    learning_rate FLOAT DEFAULT 0.01,            -- 学习率
    momentum FLOAT DEFAULT 0.937,                -- 动量
    weight_decay FLOAT DEFAULT 0.0005,           -- 权重衰减
    optimizer VARCHAR(20) DEFAULT 'SGD',         -- 优化器 (SGD/Adam/AdamW)
    lr_scheduler VARCHAR(20) DEFAULT 'cosine',   -- 学习率调度器
    warmup_epochs INT DEFAULT 3,                 -- 预热轮数
    patience INT DEFAULT 50,                     -- 早停耐心值
    additional_params JSONB,                     -- 额外参数
    
    -- ========== 增强配置 ==========
    augmentation_config JSONB,                   -- 数据增强配置
    hsv_h FLOAT DEFAULT 0.015,                   -- HSV色调增强
    hsv_s FLOAT DEFAULT 0.7,                     -- HSV饱和度增强
    hsv_v FLOAT DEFAULT 0.4,                     -- HSV亮度增强
    degrees FLOAT DEFAULT 0.0,                   -- 旋转角度
    translate FLOAT DEFAULT 0.1,                 -- 平移
    scale FLOAT DEFAULT 0.5,                     -- 缩放
    shear FLOAT DEFAULT 0.0,                     -- 剪切
    perspective FLOAT DEFAULT 0.0,               -- 透视
    flip_lr BOOLEAN DEFAULT TRUE,                -- 水平翻转
    flip_ud BOOLEAN DEFAULT FALSE,               -- 垂直翻转
    
    -- ========== 硬件配置 ==========
    gpu_ids VARCHAR(50),                         -- GPU设备ID (0,1,2或cpu)
    num_workers INT DEFAULT 4,                   -- 数据加载线程数
    mixed_precision BOOLEAN DEFAULT TRUE,        -- 混合精度训练
    
    -- ========== 任务状态 ==========
    status VARCHAR(20) DEFAULT 'PENDING',        -- PENDING/RUNNING/SUCCESS/FAILED/CANCELLED
    progress FLOAT DEFAULT 0,                    -- 进度 0-100
    current_epoch INT DEFAULT 0,                 -- 当前训练轮数
    current_step VARCHAR(100),                   -- 当前步骤描述
    
    -- ========== 训练指标 ==========
    metrics_json JSONB,                          -- 训练指标（实时）{epoch, loss, map, precision, recall}
    best_metrics JSONB,                          -- 最佳指标
    training_logs_path VARCHAR(500),             -- 训练日志路径
    tensorboard_log_path VARCHAR(500),           -- TensorBoard日志路径
    
    -- ========== 模型输出 ==========
    model_id BIGINT,                             -- 关联的模型注册ID（biz_model）
    model_path VARCHAR(500),                     -- 最终模型路径
    best_model_path VARCHAR(500),                -- 最佳模型路径
    last_model_path VARCHAR(500),                -- 最后一轮模型路径
    model_size BIGINT,                           -- 模型文件大小
    inference_time_ms FLOAT,                     -- 推理时间（毫秒）
    
    -- ========== 评估结果 ==========
    evaluation_results JSONB,                    -- 评估结果 {map50, map50_95, precision, recall}
    confusion_matrix_path VARCHAR(500),          -- 混淆矩阵图片路径
    pr_curve_path VARCHAR(500),                  -- PR曲线图片路径
    
    -- ========== 错误信息 ==========
    error_message TEXT,
    error_stack TEXT,
    
    -- ========== 审计字段 ==========
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INT,
    update_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE biz_yolo_training_task IS 'YOLO模型训练任务表';
COMMENT ON COLUMN biz_yolo_training_task.dataset_task_id IS '关联的数据集构建任务ID';
COMMENT ON COLUMN biz_yolo_training_task.model_architecture IS '模型架构（yolov8n/yolov8s/yolov8m等）';
COMMENT ON COLUMN biz_yolo_training_task.pretrained_weights IS '预训练权重路径';
COMMENT ON COLUMN biz_yolo_training_task.epochs IS '训练轮数';
COMMENT ON COLUMN biz_yolo_training_task.metrics_json IS '训练指标（JSON格式）';
COMMENT ON COLUMN biz_yolo_training_task.best_metrics IS '最佳性能指标';
COMMENT ON COLUMN biz_yolo_training_task.model_id IS '关联的模型注册ID（biz_model表）';
COMMENT ON COLUMN biz_yolo_training_task.model_path IS '最终模型路径';
COMMENT ON COLUMN biz_yolo_training_task.best_model_path IS '最佳模型路径';
COMMENT ON COLUMN biz_yolo_training_task.evaluation_results IS '评估结果';

-- 索引优化
CREATE INDEX idx_training_task_project ON biz_yolo_training_task(project_id);
CREATE INDEX idx_training_task_status ON biz_yolo_training_task(status);
CREATE INDEX idx_training_task_dataset ON biz_yolo_training_task(dataset_task_id);
CREATE INDEX idx_training_task_create_time ON biz_yolo_training_task(create_time DESC);
CREATE INDEX idx_training_task_model ON biz_yolo_training_task(model_architecture);
CREATE INDEX idx_training_task_model_id ON biz_yolo_training_task(model_id);

