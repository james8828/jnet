-- create database jnet;
-- CREATE SCHEMA jnet AUTHORIZATION sa;
-- use jnet;
drop table if exists sys_user;
create table sys_user
(
    user_id      bigint(20) auto_increment
        primary key,
    user_name    varchar(50)          not null,
    password     varchar(100)         not null comment '登录密码',
    nick_name    varchar(255)         null,
    head_img_url varchar(1024)        null,
    mobile       varchar(11)          null,
    sex          tinyint(1)           null,
    enabled      tinyint(1) default 1 not null,
    type         varchar(16)          null,
    company      varchar(255)         null,
    open_id      varchar(32)          null,
    create_by    bigint(20)           NULL,
    create_time  datetime             NULL,
    update_by    bigint(20)           NULL,
    update_time  datetime             NULL,
    del_flag     tinyint(1) DEFAULT 0 COMMENT '是否删除'
);

DROP TABLE IF EXISTS sys_client;
CREATE TABLE sys_client
(
    client_registration_id  varchar(100)                            NOT NULL,
    principal_name          varchar(200)                            NOT NULL,
    access_token_type       varchar(100)                            NOT NULL,
    access_token_value      blob                                    NOT NULL,
    access_token_issued_at  timestamp                               NOT NULL,
    access_token_expires_at timestamp                               NOT NULL,
    access_token_scopes     varchar(1000) DEFAULT NULL,
    refresh_token_value     blob          DEFAULT NULL,
    refresh_token_issued_at timestamp     DEFAULT NULL,
    created_at              timestamp     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    enabled      tinyint(1) default 1 not null,
    create_by    bigint(20)           NULL,
    create_time  datetime             NULL,
    update_by    bigint(20)           NULL,
    update_time  datetime             NULL,
    PRIMARY KEY (client_registration_id, principal_name)
);

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu
(
    menu_id     bigint(20)   AUTO_INCREMENT,
    menu_name   varchar(64)  NOT NULL COMMENT '菜单名',
    path        varchar(200) NULL COMMENT '路由地址',
    component   varchar(255) NULL COMMENT '组件路径',
    visible     tinyint(1)   NOT NULL DEFAULT 1 COMMENT '菜单显隐状态',
    enabled     tinyint(1)   NOT NULL DEFAULT 1,
    perms       varchar(100) NULL COMMENT '权限标识',
    icon        varchar(100) NOT NULL DEFAULT '#' COMMENT '菜单图标',
    create_by   bigint(20)   NULL,
    create_time datetime     NULL,
    update_by   bigint(20)   NULL,
    update_time datetime     NULL,
    del_flag    tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
    remark      varchar(500) NULL COMMENT '备注',
    PRIMARY KEY (menu_id)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜单表';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    role_id     bigint(20)   AUTO_INCREMENT,
    role_name   varchar(128) NULL,
    role_key    varchar(100) NULL COMMENT '角色权限字符串',
    enabled     tinyint(1)   NOT NULL default 1,
    del_flag    int(1)       NOT NULL DEFAULT '0' COMMENT 'del_flag',
    create_by   bigint(200)  NULL,
    create_time datetime     NULL,
    update_by   bigint(200)  NULL,
    update_time datetime     NULL,
    remark      varchar(500) NULL COMMENT '备注',
    PRIMARY KEY (role_id)
) ENGINE = InnoDB COMMENT ='角色表';

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu
(
    role_id bigint(200) NOT NULL COMMENT '角色ID',
    menu_id bigint(200) NOT NULL COMMENT '菜单id',
    PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    user_id bigint(200) NOT NULL COMMENT '用户id',
    role_id bigint(200) NOT NULL COMMENT '角色id',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

insert into sys_user(user_name, password)
values ('admin', '{bcrypt}$2a$10$vuR.WoFXNNuufU19I5isgO5VRRq5vAM8OsToIJ1HA1YsnADYjz4CW');
insert into sys_user(user_name, password)
values ('james', '{bcrypt}$2a$10$vuR.WoFXNNuufU19I5isgO5VRRq5vAM8OsToIJ1HA1YsnADYjz4CW');
insert into sys_menu(menu_name, path, component, perms)
values ('部门管理', 'dept', 'system/dept/index', 'system:dept:list');
insert into sys_menu(menu_name, path, component, perms)
values ('测试', 'test', 'system/test/index', 'system:test:list');



insert into sys_role(role_name, role_key)
values ('CEO', 'ceo');
insert into sys_role(role_name, role_key)
values ('Coder', 'coder');


insert into sys_role_menu(role_id, menu_id)
values (1, 1);
insert into sys_role_menu(role_id, menu_id)
values (1, 2);


insert into sys_user_role(role_id, user_id)
values (1, 1);
insert into sys_user_role(role_id, user_id)
values (1, 2);
