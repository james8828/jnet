create table oauth_user
(
    oauth_user_id   bigint auto_increment comment '主键id'
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


 CREATE DATABASE IF NOT EXISTS `user-center` DEFAULT CHARACTER SET = utf8;
 Use `user-center`;

 -- ----------------------------
 -- Table structure for sys_user
 -- ----------------------------
 DROP TABLE IF EXISTS `sys_user`;
 CREATE TABLE `sys_user`  (
   `user_id` int(11) NOT NULL AUTO_INCREMENT,
   `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
   `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录密码',
   `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `head_img_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `sex` tinyint(1) NULL DEFAULT NULL,
   `enabled` tinyint(1) NOT NULL DEFAULT 1,
   `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
   `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
   `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
   `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
   `open_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
   `is_del` tinyint(1) NOT NULL DEFAULT 0,
   `creator_id` int(11) COMMENT '创建人id',
   `update_id` int(11) COMMENT '修改人id',
   PRIMARY KEY (`user_id`),
   KEY `idx_username` (`user_name`),
   KEY `idx_mobile` (`mobile`),
   KEY `idx_open_id` (`open_id`)
 ) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

 -- ----------------------------
 -- Records of sys_user
 -- ----------------------------
 INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$TJkwVdlpbHKnV45.nBxbgeFHmQRmyWlshg94lFu2rKxVtT2OMniDO', '管理员', 'http://pkqtmn0p1.bkt.clouddn.com/头像.png', '18888888888', 0, 1, 'APP', '2017-11-17 16:56:59', '2019-01-08 17:05:47', 'ENGJ', '123', 0, 1);
 INSERT INTO `sys_user` VALUES (2, 'user', '$2a$10$OhfZv4VQJiqMEukpf1qXA.V7UMiHjr86g6lJqPvKUoHwrPk35steG', '体验用户', 'http://payo7kq4i.bkt.clouddn.com/QQ%E5%9B%BE%E7%89%8720180819191900.jpg', '18888888887', 1, 1, 'APP', '2017-11-17 16:56:59', NULL, 'ENGJ', NULL, 0, 1);
 INSERT INTO `sys_user` VALUES (3, 'test', '$2a$10$RD18sHNphJMmcuLuUX/Np.IV/7Ngbjd3Jtj3maFLpwaA6KaHVqPtq', '测试账户', 'http://payo7kq4i.bkt.clouddn.com/QQ%E5%9B%BE%E7%89%8720180819191900.jpg', '13851539156', 0, 0, 'APP', '2017-11-17 16:56:59', '2018-09-07 03:27:40', 'ENGJ', NULL, 0, 1);
 INSERT INTO `sys_user` VALUES (4, '1', '$2a$10$9vLdwXBZaAPy/hmzEDf.M.YbrsKWGG21nqWq17/EwWPBi65GDivLa', '11', NULL, '13530151800', 1, 1, 'APP', '2018-09-07 14:20:51', '2018-11-15 01:45:36', 'YCC', NULL, 0, 1);
 INSERT INTO `sys_user` VALUES (5, '12', '$2a$10$cgRGZ0uuIAoKuwBoTWmz7eJzP4RUEr688VlnpZ4BTCz2RZEt0jrIe', '12', NULL, '17587132062', 0, 1, 'APP', '2018-09-08 04:52:25', '2018-09-16 01:48:00', 'YCC', NULL, 0, 1);
 INSERT INTO `sys_user` VALUES (6, 'abc1', '$2a$10$pzvn4TfBh2oFZJbtagovFe56ZTUlTaawPnx0Yz2PeqGex0xbddAGu', 'abc', NULL, '12345678901', 0, 1, 'APP', '2018-09-11 08:02:25', '2018-09-14 06:49:54', 'YCC', NULL, 0, 1);
 INSERT INTO `sys_user` VALUES (7, '234', '$2a$10$FxFvGGSi2RCe4lm5V.G0Feq6szh5ArMz.8Mzm08zQlkA.VgE9GFbm', 'ddd', NULL, '13245678906', 0, 1, 'APP', '2018-09-19 01:33:54', '2018-09-19 01:33:54', 'JFSC', NULL, 1, 1);
 INSERT INTO `sys_user` VALUES (8, 'tester', '$2a$10$VUfknatgKIoZJYDLIesrrO5Vg8Djw5ON2oDWeXyC24TZ6Ca/TWiye', 'tester', NULL, '12345678901', 0, 1, 'APP', '2018-09-19 04:52:01', '2018-11-16 22:12:04', 'JFSC', NULL, 1, 1);
 INSERT INTO `sys_user` VALUES (9, '11111111111111111111', '$2a$10$DNaUDpCHKZI0V9w.R3wBaeD/gGOQDYjgC5fhju7bQLfIkqsZV61pi', 'cute', 'http://payo7kq4i.bkt.clouddn.com/C:\\Users\\GAOY91\\Pictures\\79f0f736afc37931a921fd59e3c4b74543a91170.jpg', '15599999991', 1, 1, 'APP', '2018-09-19 04:57:39', NULL, 'JFSC', NULL, 1, 1);
 INSERT INTO `sys_user` VALUES (10, 'test001', '123456', 'test001', NULL, '11111111', 0, 1, 'BACKEND', '2018-09-12 13:50:57', '2019-01-07 13:04:18', NULL, NULL, 1, 1);
 INSERT INTO `sys_user` VALUES (11, 'test002', '123456', 'test002', NULL, '22222222', 0, 1, 'BACKEND', '2018-09-11 08:02:25', '2018-09-14 06:49:54', NULL, NULL, 1, 1);
 INSERT INTO `sys_user` VALUES (12, '123', '$2a$10$PgngbC9pQWDT.ZG37fvV6e8Zi0C3mQOVMJJE35.XQULnppSEWhyPK', '12', NULL, '1', 0, 1, 'BACKEND', '2019-01-19 13:44:02', '2019-01-19 13:44:02', NULL, NULL, 1, 1);

 -- ----------------------------
 -- Table structure for sys_role
 -- ----------------------------
 DROP TABLE IF EXISTS `sys_role`;
 CREATE TABLE `sys_role`  (
   `role_id` int(11) NOT NULL AUTO_INCREMENT,
   `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色code',
   `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名',
   `data_scope` varchar(32) DEFAULT 'ALL' comment '数据权限范围配置：ALL/全部权限，CREATOR/创建者权限',
   `create_time` datetime(0) NULL DEFAULT NULL,
   `update_time` datetime(0) NULL DEFAULT NULL,
   `tenant_id` varchar(32) DEFAULT '' COMMENT '租户字段',
   `creator_id` int(11) COMMENT '创建人id',
   `update_id` int(11) COMMENT '修改人id',
   PRIMARY KEY (`role_id`),
   KEY `idx_code` (`role_code`),
   KEY `idx_tenant_id` (`tenant_id`)
 ) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

 -- ----------------------------
 -- Records of sys_role
 -- ----------------------------
 INSERT INTO `sys_role` VALUES (1, 'ADMIN', '管理员', 'ALL', '2017-11-17 16:56:59', '2018-09-19 09:39:10', 'webApp', 1);
 INSERT INTO `sys_role` VALUES (2, 'test', '测试', 'ALL', '2018-09-17 10:15:51', '2018-11-15 01:49:14', 'webApp', 1);
 INSERT INTO `sys_role` VALUES (3, '11', '11', 'ALL', '2018-11-15 01:49:19', '2018-11-15 01:49:19', 'webApp', 1);
 INSERT INTO `sys_role` VALUES (4, 'shop_admin', '商城管理员', 'ALL', '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 'zlt', 1);
 INSERT INTO `sys_role` VALUES (5, 'app_admin', '移动管理员', 'ALL', '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 'app', 1);

 -- ----------------------------
 -- Table structure for sys_role_user
 -- ----------------------------
 DROP TABLE IF EXISTS `sys_role_user`;
 CREATE TABLE `sys_role_user`  (
   `user_id` int(11) NOT NULL,
   `role_id` int(11) NOT NULL,
   PRIMARY KEY (`user_id`, `role_id`)
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

 -- ----------------------------
 -- Records of sys_role_user
 -- ----------------------------
 INSERT INTO `sys_role_user` VALUES (1, 1);
 INSERT INTO `sys_role_user` VALUES (2, 1);
 INSERT INTO `sys_role_user` VALUES (3, 1);
 INSERT INTO `sys_role_user` VALUES (4, 1);
 INSERT INTO `sys_role_user` VALUES (5, 1);
 INSERT INTO `sys_role_user` VALUES (6, 1);
 INSERT INTO `sys_role_user` VALUES (7, 2);
 INSERT INTO `sys_role_user` VALUES (8, 2);
 INSERT INTO `sys_role_user` VALUES (9, 3);
 INSERT INTO `sys_role_user` VALUES (10, 3);
 INSERT INTO `sys_role_user` VALUES (11, 4);
 INSERT INTO `sys_role_user` VALUES (12, 5);

 -- ----------------------------
 -- Table structure for sys_menu
 -- ----------------------------
 DROP TABLE IF EXISTS `sys_menu`;
 CREATE TABLE `sys_menu`  (
   `menu_id` int(11) NOT NULL AUTO_INCREMENT,
   `parent_id` int(11) NOT NULL,
   `menu_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
   `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `path_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `css` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
   `sort` int(11) NOT NULL,
   `create_time` datetime(0) NULL,
   `update_time` datetime(0) NULL,
   `type` tinyint(1) NOT NULL,
   `hidden` tinyint(1) NOT NULL DEFAULT 0,
   `tenant_id` varchar(32) DEFAULT '' COMMENT '租户字段',
   `creator_id` int(11) COMMENT '创建人id',
   `update_id` int(11) COMMENT '修改人id',
   PRIMARY KEY (`menu_id`),
   KEY `idx_parent_id` (`parent_id`),
   KEY `idx_tenant_id` (`tenant_id`)
 ) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

 -- ----------------------------
 -- Records of sys_menu
 -- ----------------------------
 INSERT INTO `sys_menu` VALUES (2, 12, '用户管理', '#!user', 'system/user.html', NULL, 'layui-icon-friends', 2, '2017-11-17 16:56:59', '2018-09-19 11:26:14', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (3, 12, '角色管理', '#!role', 'system/role.html', NULL, 'layui-icon-user', 3, '2017-11-17 16:56:59', '2019-01-14 15:34:40', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (4, 12, '菜单管理', '#!menus', 'system/menus.html', NULL, 'layui-icon-menu-fill', 4, '2017-11-17 16:56:59', '2018-09-03 02:23:47', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (9, 37, '文件中心', '#!files', 'files/files.html', NULL, 'layui-icon-file', 3, '2017-11-17 16:56:59', '2019-01-17 20:18:44', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (10, 37, '文档中心', '#!swagger', 'http://127.0.0.1:9900/doc.html', NULL, 'layui-icon-app', 4, '2017-11-17 16:56:59', '2019-01-17 20:18:48', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (11, 12, '我的信息', '#!myInfo', 'system/myInfo.html', NULL, 'layui-icon-login-qq', 10, '2017-11-17 16:56:59', '2018-09-02 06:12:24', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (12, -1, '认证管理', 'javascript:;', '', NULL, 'layui-icon-set', 1, '2017-11-17 16:56:59', '2018-12-13 15:02:49', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (35, 12, '应用管理', '#!app', 'attestation/app.html', NULL, 'layui-icon-link', 5, '2017-11-17 16:56:59', '2019-01-14 15:35:15', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (37, -1, '系统管理', 'javascript:;', '', NULL, 'layui-icon-set', 2, '2018-08-25 10:41:58', '2019-01-23 14:01:58', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (62, 63, '应用监控', '#!admin', 'http://127.0.0.1:6500/#/wallboard', NULL, 'layui-icon-chart-screen', 4, '2019-01-08 15:32:19', '2019-01-17 20:22:44', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (63, -1, '系统监控', 'javascript:;', '', NULL, 'layui-icon-set', 2, '2019-01-10 18:35:05', '2019-01-10 18:35:05', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (64, 63, '系统日志', '#!sysLog', 'log/sysLog.html', NULL, 'layui-icon-file-b', 1, '2019-01-10 18:35:55', '2019-01-12 00:27:20', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (65, 37, '代码生成器', '#!generator', 'generator/list.html', NULL, 'layui-icon-template', 2, '2019-01-14 00:47:36', '2019-01-23 14:06:31', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (66, 63, '慢查询SQL', '#!slowQueryLog', 'log/slowQueryLog.html', NULL, 'layui-icon-snowflake', 2, '2019-01-16 12:00:27', '2019-01-16 15:32:31', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (67, -1, '任务管理', '#!job', 'http://127.0.0.1:8081/', NULL, 'layui-icon-date', 3, '2019-01-17 20:18:22', '2019-01-23 14:01:53', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (68, 63, '应用吞吐量监控', '#!sentinel', 'http://127.0.0.1:6999', NULL, 'layui-icon-chart', 5, '2019-01-22 16:31:55', '2019-01-22 16:34:03', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (69, 37, '配置中心', '#!nacos', 'http://127.0.0.1:8848/nacos', NULL, 'layui-icon-tabs', 1, '2019-01-23 14:06:10', '2019-01-23 14:06:10', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (70, 63, 'APM监控', '#!apm', 'http://127.0.0.1:8080', null, 'layui-icon-engine', 6, '2019-02-27 10:31:55', '2019-02-27 10:31:55', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (71, -1, '搜索管理', 'javascript:;', '', NULL, 'layui-icon-set', 3, '2018-08-25 10:41:58', '2019-01-23 15:07:07', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (72, 71, '索引管理', '#!index', 'search/index_manager.html', NULL, 'layui-icon-template', 1, '2019-01-10 18:35:55', '2019-01-12 00:27:20', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (73, 71, '用户搜索', '#!userSearch', 'search/user_search.html', NULL, 'layui-icon-user', 2, '2019-01-10 18:35:55', '2019-01-12 00:27:20', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (74, 12, 'Token管理', '#!tokens', 'system/tokens.html', NULL, 'layui-icon-unlink', 6, '2019-07-11 16:56:59', '2019-07-11 16:56:59', 1, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (75, 2, '用户列表', '/api-user/users', 'user-list', 'GET', null, 1, '2019-07-29 16:56:59', '2019-07-29 16:56:59', 2, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (76, 2, '查询用户角色', '/api-user/roles', 'user-roles', 'GET', null, 2, '2019-07-29 16:56:59', '2019-07-29 16:56:59', 2, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (77, 2, '用户添加', '/api-user/users/saveOrUpdate', 'user-btn-add', 'POST', null, 3, '2019-07-29 16:56:59', '2019-07-29 16:56:59', 2, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (78, 2, '用户导出', '/api-user/users/export', 'user-btn-export', 'POST', null, 4, '2019-07-29 16:56:59', '2019-07-29 16:56:59', 2, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (79, 2, '用户导入', '/api-user/users/import', 'user-btn-import', 'POST', null, 5, '2019-07-29 16:56:59', '2019-07-29 16:56:59', 2, 0, 'webApp', 1);
 INSERT INTO `sys_menu` VALUES (80, -1, '用户管理', '#!user', '', NULL, NULL, 1, '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 1, 0, 'zlt', 1);
 INSERT INTO `sys_menu` VALUES (81, -1, '商品管理', '#!product', '', NULL, NULL, 2, '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 1, 0, 'zlt', 1);
 INSERT INTO `sys_menu` VALUES (82, -1, '支付管理', '#!pay', '', NULL, NULL, 3, '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 1, 0, 'zlt', 1);
 INSERT INTO `sys_menu` VALUES (83, -1, '交易管理', '#!trading', '', NULL, NULL, 4, '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 1, 0, 'zlt', 1);
 INSERT INTO `sys_menu` VALUES (84, -1, '系统管理', '#!system', '', NULL, NULL, 1, '2019-08-06 20:02:12.604', '2019-08-06 20:02:12.604', 1, 0, 'app', 1);
 INSERT INTO `sys_menu` VALUES (85, 63, '审计日志', '#!auditLog', 'log/auditLog.html', NULL, 'layui-icon-file-b', 3, '2020-02-04 12:00:27', '2020-02-04 15:32:31', 1, 0, 'webApp', 1);

 -- ----------------------------
 -- Table structure for sys_role_menu
 -- ----------------------------
 DROP TABLE IF EXISTS `sys_role_menu`;
 CREATE TABLE `sys_role_menu`  (
   `role_id` int(11) NOT NULL,
   `menu_id` int(11) NOT NULL,
   PRIMARY KEY (`role_id`, `menu_id`)
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

 -- ----------------------------
 -- Records of sys_role_menu
 -- ----------------------------
 INSERT INTO `sys_role_menu` VALUES (1, 2);
 INSERT INTO `sys_role_menu` VALUES (1, 3);
 INSERT INTO `sys_role_menu` VALUES (1, 4);
 INSERT INTO `sys_role_menu` VALUES (1, 9);
 INSERT INTO `sys_role_menu` VALUES (1, 10);
 INSERT INTO `sys_role_menu` VALUES (1, 11);
 INSERT INTO `sys_role_menu` VALUES (1, 12);
 INSERT INTO `sys_role_menu` VALUES (1, 35);
 INSERT INTO `sys_role_menu` VALUES (1, 37);
 INSERT INTO `sys_role_menu` VALUES (1, 62);
 INSERT INTO `sys_role_menu` VALUES (1, 63);
 INSERT INTO `sys_role_menu` VALUES (1, 64);
 INSERT INTO `sys_role_menu` VALUES (1, 65);
 INSERT INTO `sys_role_menu` VALUES (1, 66);
 INSERT INTO `sys_role_menu` VALUES (1, 67);
 INSERT INTO `sys_role_menu` VALUES (1, 68);
 INSERT INTO `sys_role_menu` VALUES (1, 69);
 INSERT INTO `sys_role_menu` VALUES (1, 70);
 INSERT INTO `sys_role_menu` VALUES (1, 71);
 INSERT INTO `sys_role_menu` VALUES (1, 72);
 INSERT INTO `sys_role_menu` VALUES (1, 73);
 INSERT INTO `sys_role_menu` VALUES (1, 74);
 INSERT INTO `sys_role_menu` VALUES (1, 75);
 INSERT INTO `sys_role_menu` VALUES (1, 76);
 INSERT INTO `sys_role_menu` VALUES (1, 77);
 INSERT INTO `sys_role_menu` VALUES (1, 78);
 INSERT INTO `sys_role_menu` VALUES (1, 79);
 INSERT INTO `sys_role_menu` VALUES (1, 85);
 INSERT INTO `sys_role_menu` VALUES (2, 2);
 INSERT INTO `sys_role_menu` VALUES (2, 3);
 INSERT INTO `sys_role_menu` VALUES (2, 4);
 INSERT INTO `sys_role_menu` VALUES (2, 11);
 INSERT INTO `sys_role_menu` VALUES (2, 12);
 INSERT INTO `sys_role_menu` VALUES (2, 35);
 INSERT INTO `sys_role_menu` VALUES (3, 2);
 INSERT INTO `sys_role_menu` VALUES (3, 3);
 INSERT INTO `sys_role_menu` VALUES (3, 4);
 INSERT INTO `sys_role_menu` VALUES (3, 12);
 INSERT INTO `sys_role_menu` VALUES (4, 80);
 INSERT INTO `sys_role_menu` VALUES (4, 81);
 INSERT INTO `sys_role_menu` VALUES (4, 82);
 INSERT INTO `sys_role_menu` VALUES (4, 83);
 INSERT INTO `sys_role_menu` VALUES (5, 84);

    CREATE DATABASE IF NOT EXISTS `oauth-center` DEFAULT CHARACTER SET = utf8;
    Use `oauth-center`;

    -- ----------------------------
    -- Table structure for oauth_client_details
    -- ----------------------------
    DROP TABLE IF EXISTS `oauth_client_details`;
    CREATE TABLE `oauth_client_details`  (
      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
      `client_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '应用标识',
      `resource_ids` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '资源限定串(逗号分割)',
      `client_secret` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应用密钥(bcyt) 加密',
      `client_secret_str` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应用密钥(明文)',
      `scope` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '范围',
      `authorized_grant_types` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '5种oauth授权方式(authorization_code,password,refresh_token,client_credentials)',
      `web_server_redirect_uri` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '回调地址 ',
      `authorities` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限',
      `access_token_validity` int(11) NULL DEFAULT NULL COMMENT 'access_token有效期',
      `refresh_token_validity` int(11) NULL DEFAULT NULL COMMENT 'refresh_token有效期',
      `additional_information` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '{}' COMMENT '{}',
      `auto_approve` char(5) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'true' COMMENT '是否自动授权 是-true',
      `create_time` datetime(0) NULL DEFAULT NULL,
      `update_time` datetime(0) NULL DEFAULT NULL,
      `client_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '应用名称',
      `token_format` varchar(20) not null DEFAULT 'reference' COMMENT 'token格式: reference 引用令牌(不透明), self-contained 自包含令牌(jwt))',
      `creator_id` int(11) COMMENT '创建人id',
      PRIMARY KEY (`id`) USING BTREE
    ) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

    -- ----------------------------
    -- Records of oauth_client_details
    -- ----------------------------
    INSERT INTO `oauth_client_details` VALUES (1, 'webApp', NULL, '$2a$10$06msMGYRH8nrm4iVnKFNKOoddB8wOwymVhbUzw/d3ZixD7Nq8ot72', 'webApp', 'app,openid,profile', 'authorization_code,password,refresh_token,client_credentials,implicit,password_code,openId,mobile_password', 'http://127.0.0.1:8082/callback.html', NULL, 3600, NULL, '{"LOGOUT_NOTIFY_URL_LIST":"http://127.0.0.1:8082/logoutNotify"}', 'true', NULL, NULL, 'pc端', 'reference', 1);
    INSERT INTO `oauth_client_details` VALUES (2, 'app', NULL, '$2a$10$i3F515wEDiB4Gvj9ym9Prui0dasRttEUQ9ink4Wpgb4zEDCAlV8zO', 'app', 'app', 'authorization_code,password,refresh_token', 'http://127.0.0.1:8081/callback.html', NULL, 3600, NULL, '{"LOGOUT_NOTIFY_URL_LIST":"http://127.0.0.1:8081/logoutNotify"}', 'true', NULL, NULL, '移动端', 'reference', 1);
    INSERT INTO `oauth_client_details` VALUES (3, 'zlt', NULL, '$2a$10$/o.wuORzVcXaezmYVzwYMuoY7qeWXBALwQmkskXD/7C6rqfCyPrna', 'zlt', 'all,openid,profile', 'authorization_code,password,refresh_token,client_credentials', 'http://127.0.0.1:8080/singleLogin', NULL, 3600, 28800, '{}', 'true', '2018-12-27 00:50:30', '2018-12-27 00:50:30', '第三方应用', 'reference', 1);