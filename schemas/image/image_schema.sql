create table img_attachment
(
    attachment_id   bigint auto_increment comment '主键id'
        primary key,
    attachment_type char     default '1'               null comment '附件类型：1、普通附件 2、报表统计',
    data_status     char                               null comment '数据状态：1、不可用，2、可用',
    attachment_name varchar(4096)                      not null comment '附件名称',
    attachment_code varchar(255)                       not null comment '附件编号',
    attachment_size bigint                             null comment '附件大小',
    attachment_path varchar(4096)                      null comment '附件位置',
    attachment_md5  varchar(32)                        null comment 'MD5值',
    attachment_ext  varchar(255)                       null comment '文件后缀名',
    create_by       bigint                             null comment '创建者',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_by       bigint                             null comment '更新者',
    update_time     datetime default CURRENT_TIMESTAMP null comment '更新时间'
)
    comment '附件表' row_format = DYNAMIC;
create index attachment_code_index
    on img_attachment (attachment_code);


create table img_image
(
    image_id        bigint auto_increment comment '图像ID'
        primary key,
    file_name       varchar(200)                           null comment '无扩展名文件名称',
    image_name      varchar(200)                           null comment '图像名称（文件名）',
    image_path      varchar(200)                           null comment '图像的绝对路径',
    image_url       varchar(200)                           null comment '图像URL地址',
    thumb_url       varchar(200)                           null comment '缩略图URL地址',
    macro_url       varchar(200)                           null comment 'macro图片URL地址',
    label_url       varchar(200)                           null comment 'label图片RUL地址',
    cache_url       varchar(255)                           null comment '1024缩略图路径（用于缓存、标注缩略图时需要）',
    multiple        varchar(255)                           null comment '原图缩到cache图的倍数',
    format          varchar(200)                           null comment '文件格式',
    width           varchar(200)                           null comment '宽度',
    height          varchar(200)                           null comment '高度',
    depth           varchar(200)                           null comment '深度',
    size            varchar(200)                           null comment '大小',
    global_size     varchar(200)                           null comment '全局大小',
    resolving_power varchar(200)                           null comment '分辨率',
    tile_count_list varchar(200)                           null comment '每层的切片个数',
    level_count     tinyint unsigned                       null comment '总层数（小于2则失败）',
    chunk_total     smallint unsigned                      null comment '前端总切片个数',
    md5             varchar(200)                           null comment '图片的MD5值',
    resolution_x    varchar(255)                           null comment 'x轴分辨率',
    resolution_y    varchar(255)                           null comment 'y轴分辨率',
    source_lens     int                                    null comment '原放大倍数',
    process_flag    char         default '5'               not null comment '处理状态，处理状态，1解析中、2解析失败、3可用、4上传失败、5上传中',
    create_by       bigint       default 0                 not null comment '创建者',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_by       bigint       default 0                 not null comment '更新者',
    update_time     datetime     default CURRENT_TIMESTAMP not null comment '更新时间',
    image_code      varchar(255)                           null comment '图片（切片）编号',
    topic_id        bigint                                 null comment '专题ID',
    topic_code      varchar(255) default ''                not null comment '专题编号',
    status          char                                   null comment '是否可用0不可用1可用',
    del_flag        char         default '0'               not null comment '删除标志（0代表存在 1代表删除）',
    biz_type        tinyint                                null comment '业务类型（1原始切片（默认）、2预测切片）',
    source          tinyint      default 1                 not null comment '图像来源(1手动上传，2服务器读取)',
    archive_status  char         default '1'               not null comment '状态(1未归档 2已归档)'
)
    comment '图像信息' row_format = DYNAMIC;

create index file_name_index
    on img_image (file_name);

create index name_index
    on img_image (image_name);

create index topic_code_index
    on img_image (topic_code);

