-- ============================================================
-- NovaNet UI 登录数据初始化脚本
-- 目标：创建可用的 UI 登录用户并启用必要的服务
-- 数据库：Sybase SQL Anywhere 12 (DBA schema)
-- ============================================================

-- 1. OEM 签名认证（必须先执行，否则写入可能失败）
SET OPTION PUBLIC.OEM_SIGNATURE = 
    'Company=Nova Biomedical;Application=NovaNet;Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158';

COMMIT;

-- ============================================================
-- 2. 检查并创建 INSTRUMENT_TYPES 记录
--    operator_privilege 有外键约束引用此表
-- ============================================================
INSERT INTO dba.INSTRUMENT_TYPES (inst_type, does_remote_review, inst_class, use_inst_lot_data)
SELECT 'MTE', 'F', 'Admin', 'F'
WHERE NOT EXISTS (SELECT 1 FROM dba.INSTRUMENT_TYPES WHERE inst_type = 'MTE');

INSERT INTO dba.INSTRUMENT_TYPES (inst_type, does_remote_review, inst_class, use_inst_lot_data)
SELECT 'NOVA', 'F', 'Analyzer', 'F'
WHERE NOT EXISTS (SELECT 1 FROM dba.INSTRUMENT_TYPES WHERE inst_type = 'NOVA');

COMMIT;

-- ============================================================
-- 3. 检查并创建 inst_locations 记录
--    operator_to_unit 有外键约束引用此表
-- ============================================================
INSERT INTO dba.inst_locations (loc_num, parent, level_num, loc_name, is_default)
SELECT NEWID(), '0', 1, 'NovaNet Lab', 'T'
WHERE NOT EXISTS (SELECT 1 FROM dba.inst_locations WHERE loc_name = 'NovaNet Lab');

COMMIT;

-- 获取刚创建的 location GUID（用于后续插入）
-- 如果已存在则使用已有的
SELECT loc_num FROM dba.inst_locations WHERE loc_name = 'NovaNet Lab';

-- ============================================================
-- 4. 创建操作员（UI 登录用户）
--    operator_id 即登录用户名
-- ============================================================
-- 先清理可能存在的同名旧用户
DELETE FROM dba.operator_privilege 
WHERE operator_num IN (SELECT operator_num FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin');
DELETE FROM dba.operator_to_unit 
WHERE operator_num IN (SELECT operator_num FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin');
DELETE FROM dba.contact_info 
WHERE contact_num IN (SELECT operator_num FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin' AND ref_table = 'OPERATORS');
DELETE FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin';

INSERT INTO dba.OPERATORS (operator_num, operator_id, is_supervisor, arch, add_date, last_update_date)
VALUES (NEWID(), 'NovaAdmin', 'T', 'F', CURRENT TIMESTAMP, CURRENT TIMESTAMP);

COMMIT;

-- 获取刚创建的 operator GUID
SELECT operator_num, operator_id FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin';

-- ============================================================
-- 5. 创建联系人信息
-- ============================================================
INSERT INTO dba.contact_info (contact_num, last_name, first_name, initials, email, ref_table)
SELECT operator_num, 'Administrator', 'Nova', 'NA', 'admin@nova.local', 'OPERATORS'
FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin';

COMMIT;

-- ============================================================
-- 6. 创建操作员权限和密码记录
--    密码以明文存储（与 C# 代码 DBOperatorPrivilege.cs 一致）
--    inst_type = 'MTE' 通配符，匹配任何仪器类型
--    privilege = 1 超级管理员权限
--    test_name = 'NOVA' 通用测试名
-- ============================================================
INSERT INTO dba.operator_privilege 
    (operator_num, inst_type, privilege, pswd, pswd_expire_date, is_active, test_name,
     cert_start_date, cert_end_date, last_update_date)
SELECT operator_num, 'MTE', 1, 'Nova2026!', NULL, 'T', 'NOVA',
       CURRENT DATE, DATEADD(YY, 10, CURRENT DATE), CURRENT TIMESTAMP
FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin';

-- 同时为 NOVA 仪器类型创建权限记录
INSERT INTO dba.operator_privilege 
    (operator_num, inst_type, privilege, pswd, pswd_expire_date, is_active, test_name,
     cert_start_date, cert_end_date, last_update_date)
SELECT operator_num, 'NOVA', 1, 'Nova2026!', NULL, 'T', 'NOVA',
       CURRENT DATE, DATEADD(YY, 10, CURRENT DATE), CURRENT TIMESTAMP
FROM dba.OPERATORS WHERE operator_id = 'NovaAdmin';

COMMIT;

-- ============================================================
-- 7. 关联操作员到工作地点
-- ============================================================
INSERT INTO dba.operator_to_unit (operator_num, loc_num, is_default, is_active)
SELECT o.operator_num, l.loc_num, 'T', 'T'
FROM dba.OPERATORS o
CROSS JOIN dba.inst_locations l
WHERE o.operator_id = 'NovaAdmin' 
  AND l.loc_name = 'NovaNet Lab'
  AND NOT EXISTS (
    SELECT 1 FROM dba.operator_to_unit o2u 
    WHERE o2u.operator_num = o.operator_num AND o2u.loc_num = l.loc_num
  );

COMMIT;

-- ============================================================
-- 8. 启用授权服务（UI、Apache、WebServer）
--    如果 authorized_services 表为空则插入
--    如果已有记录则更新 enabled 为 'T'
-- ============================================================

-- 插入默认的 POP 名称（如果不存在）
INSERT INTO dba.authorized_services (service_name, pop_name, enabled)
SELECT 'UI', 'DefaultPOP', 'T'
WHERE NOT EXISTS (SELECT 1 FROM dba.authorized_services WHERE service_name = 'UI');

INSERT INTO dba.authorized_services (service_name, pop_name, enabled)
SELECT 'Apache', 'DefaultPOP', 'T'
WHERE NOT EXISTS (SELECT 1 FROM dba.authorized_services WHERE service_name = 'Apache');

INSERT INTO dba.authorized_services (service_name, pop_name, enabled)
SELECT 'WebServer', 'DefaultPOP', 'T'
WHERE NOT EXISTS (SELECT 1 FROM dba.authorized_services WHERE service_name = 'WebServer');

INSERT INTO dba.authorized_services (service_name, pop_name, enabled)
SELECT 'RTMBackend', 'DefaultPOP', 'T'
WHERE NOT EXISTS (SELECT 1 FROM dba.authorized_services WHERE service_name = 'RTMBackend');

-- 确保所有服务都已启用
UPDATE dba.authorized_services SET enabled = 'T' WHERE enabled != 'T';

COMMIT;

-- ============================================================
-- 9. 验证数据
-- ============================================================
PRINT '===== 验证初始化结果 =====';

PRINT '--- 操作员列表 ---';
SELECT o.operator_id, o.is_supervisor, o.add_date,
       c.last_name, c.first_name, c.email
FROM dba.OPERATORS o
LEFT JOIN dba.contact_info c ON c.contact_num = o.operator_num AND c.ref_table = 'OPERATORS';

PRINT '--- 操作员权限 ---';
SELECT o.operator_id, op.inst_type, op.privilege, op.pswd, op.is_active, op.test_name
FROM dba.operator_privilege op
JOIN dba.OPERATORS o ON o.operator_num = op.operator_num;

PRINT '--- 操作员-地点关联 ---';
SELECT o.operator_id, l.loc_name, o2u.is_active
FROM dba.operator_to_unit o2u
JOIN dba.OPERATORS o ON o.operator_num = o2u.operator_num
JOIN dba.inst_locations l ON l.loc_num = o2u.loc_num;

PRINT '--- 授权服务状态 ---';
SELECT service_name, pop_name, enabled, runtime_error, error_descr 
FROM dba.authorized_services;

PRINT '--- 可用仪器类型 ---';
SELECT inst_type, inst_class FROM dba.INSTRUMENT_TYPES;

PRINT '--- 可用地点 ---';
SELECT loc_num, loc_name, level_num, is_default FROM dba.inst_locations;

PRINT '===== 初始化完成 =====';
PRINT '';
PRINT '登录信息：';
PRINT '  用户名: NovaAdmin';
PRINT '  密  码: Nova2026!';
PRINT '  访问:   http://localhost:8888/';
