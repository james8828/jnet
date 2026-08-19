package com.nova.bioconnect;

import com.sybase.jdbc3.jdbc.SybDataSource;

import java.sql.*;

/**
 * NovaNet Sybase SQL Anywhere 数据库连接示例
 *
 * <p>使用 SybDataSource 方式连接，支持 OEM 认证和多种查询。
 *
 * JDBC 驱动类：com.sybase.jdbc3.jdbc.SybDriver
 * JDBC URL 格式：jdbc:sybase:Tds:host:port?ServiceName=xxx
 */
public class NovaNetDbExample {

    private static final String DB_HOST = "172.31.0.52";
    private static final int DB_PORT = 26383;
    private static final String DBA_USER = "dba";
    private static final String DBA_PASSWORD = "ab3dq@RND";
    private static final String BACKEND_USER = "rtmbackend";
    private static final String BACKEND_PASSWORD = "ab3dq@B3S";

    private static final String OEM_AUTH_SQL =
        "SET TEMPORARY OPTION CONNECTION_AUTHENTICATION=" +
        "'Company=Nova Biomedical;" +
        "Application=NovaNet;" +
        "Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158'";

    public static void main(String[] args) {
        System.out.println("=== NovaNet SQL Anywhere 连接示例 ===");
        System.out.println();

        // 连接 Runtime 数据库
        testDataSourceConnection("rtmbackend", DBA_USER, DBA_PASSWORD);

        // 连接其他数据库
        testOtherDatabases();

        // 使用 rtmbackend 用户连接
        System.out.println("\n=== 使用 rtmbackend 用户连接 ===");
        testDataSourceConnection("rtmbackend", BACKEND_USER, BACKEND_PASSWORD);

        // 建表与 CRUD 测试
        System.out.println("\n========================================");
        System.out.println("=== 建表与 CRUD 测试 ===");
        System.out.println("========================================");
        testCreateTableAndCrud("rtmbackend", DBA_USER, DBA_PASSWORD);
    }

    /**
     * 使用 SybDataSource 连接并执行查询
     */
    private static void testDataSourceConnection(String databaseName, String user, String password) {
        try {
            SybDataSource ds = new SybDataSource();
            ds.setServerName(DB_HOST);
            ds.setPortNumber(DB_PORT);
            ds.setDatabaseName(databaseName);
            ds.setUser(user);
            ds.setPassword(password);

            System.out.println("  连接 " + DB_HOST + ":" + DB_PORT + "/" + databaseName +
                    " (用户: " + user + ")...");

            try (Connection conn = ds.getConnection()) {
                System.out.println("  ✓ 连接成功!");

                runOemAuthentication(conn);
                queryVersionInfo(conn);
                queryTables(conn);
                queryPatients(conn);
                queryPatientVisits(conn);
                queryHealthPing(conn);
                queryInstLocations(conn);

            } catch (SQLException e) {
                System.err.println("  ✗ 连接失败: " + e.getMessage());
                printSqlException(e);
            }
        } catch (Exception e) {
            System.err.println("  ✗ 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 测试连接其他数据库
     */
    private static void testOtherDatabases() {
        String[] databases = {"history", "anywhere_strings", "profile_track", "metrics"};
        for (String db : databases) {
            try {
                SybDataSource ds = new SybDataSource();
                ds.setServerName(DB_HOST);
                ds.setPortNumber(DB_PORT);
                ds.setDatabaseName(db);
                ds.setUser(DBA_USER);
                ds.setPassword(DBA_PASSWORD);

                try (Connection conn = ds.getConnection()) {
                    runOemAuthentication(conn);
                    System.out.println("  ✓ " + db + " - 连接成功");
                }
            } catch (SQLException e) {
                System.out.println("  ✗ " + db + " - " + e.getMessage());
            }
        }
    }

    /**
     * 执行 OEM 认证
     */
    private static void runOemAuthentication(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(OEM_AUTH_SQL);
            System.out.println("  ✓ OEM 认证成功");
        } catch (SQLException e) {
            System.err.println("  ✗ OEM 认证失败: " + e.getMessage());
        }
    }

    /**
     * 查询版本信息
     */
    private static void queryVersionInfo(Connection conn) throws SQLException {
        System.out.println("\n  --- 版本信息 ---");
        String sql = "SELECT Object_Name, Version FROM DBA.version_info";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("  %-30s %s%n", rs.getString("Object_Name"), rs.getString("Version"));
            }
        }
    }

    /**
     * 列出所有表
     */
    private static void queryTables(Connection conn) throws SQLException {
        System.out.println("\n  --- 数据表列表 ---");
        DatabaseMetaData meta = conn.getMetaData();
        String[] types = {"TABLE"};
        try (ResultSet rs = meta.getTables(null, "DBA", "%", types)) {
            int count = 0;
            while (rs.next() && count < 30) {
                System.out.println("  " + rs.getString("TABLE_NAME"));
                count++;
            }
            System.out.println("  (共 " + count + " 张表)");
        }
    }

    /**
     * 查询患者表
     */
    private static void queryPatients(Connection conn) throws SQLException {
        System.out.println("\n  --- 患者列表 ---");
        try {
            String sql = "SELECT TOP 10 patient_id, medrec_num, last_name, first_name, " +
                    "sex, birthdate, facil_num, add_date " +
                    "FROM DBA.patients WHERE arch = 'F'";
            printQueryResult(conn, sql);
        } catch (SQLException e) {
            System.out.println("  查询患者失败: " + e.getMessage());
        }
    }

    /**
     * 查询就诊记录
     */
    private static void queryPatientVisits(Connection conn) throws SQLException {
        System.out.println("\n  --- 就诊记录 ---");
        try {
            String sql = "SELECT TOP 5 visit_num, patient_uuid, account_uuid, " +
                    "admit_time, discharge_time, loc_num, " +
                    "Attend_Physician, patient_type " +
                    "FROM DBA.patient_visits WHERE arch = 'F'";
            printQueryResult(conn, sql);
        } catch (SQLException e) {
            System.out.println("  查询就诊记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询健康心跳
     */
    private static void queryHealthPing(Connection conn) throws SQLException {
        System.out.println("\n  --- 健康心跳 ---");
        try {
            String sql = "SELECT TOP 10 process_name, host, update_time, do_log, " +
                    "num_messages_processed, tot_messages_processed " +
                    "FROM DBA.health_ping";
            printQueryResult(conn, sql);
        } catch (SQLException e) {
            System.out.println("  查询健康心跳失败: " + e.getMessage());
        }
    }

    /**
     * 查询机构位置
     */
    private static void queryInstLocations(Connection conn) throws SQLException {
        System.out.println("\n  --- 机构位置 ---");
        try {
            String sql = "SELECT TOP 10 loc_num, parent, level_num, loc_name, " +
                    "last_pat_update, last_op_update " +
                    "FROM DBA.inst_locations";
            printQueryResult(conn, sql);
        } catch (SQLException e) {
            System.out.println("  查询机构位置失败: " + e.getMessage());
        }
    }

    /**
     * 通用查询结果打印
     */
    private static void printQueryResult(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            int rowCount = 0;

            // 打印列名
            StringBuilder header = new StringBuilder("  ");
            for (int i = 1; i <= colCount; i++) {
                header.append(String.format("%-20s", meta.getColumnLabel(i)));
            }
            System.out.println(header);
            System.out.println("  " + "-".repeat(colCount * 20));

            // 打印数据
            while (rs.next() && rowCount < 10) {
                StringBuilder line = new StringBuilder("  ");
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getObject(i) == null ? "NULL" : rs.getObject(i).toString();
                    if (val.length() > 18) val = val.substring(0, 18) + "..";
                    line.append(String.format("%-20s", val));
                }
                System.out.println(line);
                rowCount++;
            }
            System.out.println("  共 " + rowCount + " 行");
        }
    }

    /**
     * 打印 SQL 异常详情
     */
    private static void printSqlException(SQLException e) {
        System.err.println("  SQLState: " + e.getSQLState());
        System.err.println("  ErrorCode: " + e.getErrorCode());
        if (e.getCause() != null) {
            System.err.println("  Cause: " + e.getCause().getMessage());
        }
    }

    // ==================== 建表与 CRUD 测试 ====================

    private static final String TEST_TABLE = "test_crud_nova";

    /**
     * 建表并使用 CRUD 操作进行全面测试
     */
    private static void testCreateTableAndCrud(String databaseName, String user, String password) {
        try {
            SybDataSource ds = new SybDataSource();
            ds.setServerName(DB_HOST);
            ds.setPortNumber(DB_PORT);
            ds.setDatabaseName(databaseName);
            ds.setUser(user);
            ds.setPassword(password);

            try (Connection conn = ds.getConnection()) {
                System.out.println("  ✓ 连接 " + databaseName + " 成功");

                runOemAuthentication(conn);

                // 1. 删除旧表（如果存在）
                dropTableIfExists(conn);

                // 2. 建表
                createTable(conn);

                // 3. INSERT 测试
                insertTestData(conn);

                // 4. SELECT 测试
                selectTestData(conn);

                // 5. UPDATE 测试
                updateTestData(conn);

                // 6. SELECT 验证更新
                selectTestData(conn);

                // 7. DELETE 测试
                deleteTestData(conn);

                // 8. SELECT 验证删除
                selectTestData(conn);

                // 9. 清理：删除测试表
                dropTableIfExists(conn);

                System.out.println("\n  ✓✓✓ CRUD 测试全部完成!");
            } catch (SQLException e) {
                System.err.println("  ✗ CRUD 测试失败: " + e.getMessage());
                printSqlException(e);
            }
        } catch (Exception e) {
            System.err.println("  ✗ 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 删除测试表（如果存在）
     */
    private static void dropTableIfExists(Connection conn) {
        String sql = "DROP TABLE IF EXISTS DBA." + TEST_TABLE;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("  ✓ 旧表 " + TEST_TABLE + " 已清理（如存在）");
        } catch (SQLException e) {
            System.out.println("  - 清理旧表: " + e.getMessage());
        }
    }

    /**
     * 创建测试表
     */
    private static void createTable(Connection conn) throws SQLException {
        System.out.println("\n  --- 1. 建表 ---");
        String sql = "CREATE TABLE DBA." + TEST_TABLE + " (" +
                "id INT NOT NULL PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(200), " +
                "age INT DEFAULT 0, " +
                "salary DECIMAL(10, 2) DEFAULT 0.00, " +
                "active CHAR(1) DEFAULT 'T', " +
                "create_time TIMESTAMP DEFAULT CURRENT TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT TIMESTAMP" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("  ✓ 表 " + TEST_TABLE + " 创建成功");
        }
    }

    /**
     * INSERT 测试：插入多条数据，使用 PreparedStatement 和 Statement 两种方式
     */
    private static void insertTestData(Connection conn) throws SQLException {
        System.out.println("\n  --- 2. INSERT 测试 ---");

        // 使用 PreparedStatement 插入
        String insertSql = "INSERT INTO DBA." + TEST_TABLE +
                " (id, name, email, age, salary, active) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            // 插入第1条
            pstmt.setInt(1, 1);
            pstmt.setString(2, "张三");
            pstmt.setString(3, "zhangsan@example.com");
            pstmt.setInt(4, 28);
            pstmt.setBigDecimal(5, new java.math.BigDecimal("8500.50"));
            pstmt.setString(6, "T");
            pstmt.executeUpdate();
            System.out.println("  ✓ INSERT 第1条: id=1, name=张三");

            // 插入第2条
            pstmt.setInt(1, 2);
            pstmt.setString(2, "李四");
            pstmt.setString(3, "lisi@example.com");
            pstmt.setInt(4, 35);
            pstmt.setBigDecimal(5, new java.math.BigDecimal("12000.00"));
            pstmt.setString(6, "T");
            pstmt.executeUpdate();
            System.out.println("  ✓ INSERT 第2条: id=2, name=李四");

            // 插入第3条
            pstmt.setInt(1, 3);
            pstmt.setString(2, "王五");
            pstmt.setString(3, "wangwu@example.com");
            pstmt.setInt(4, 42);
            pstmt.setBigDecimal(5, new java.math.BigDecimal("15000.00"));
            pstmt.setString(6, "F");
            pstmt.executeUpdate();
            System.out.println("  ✓ INSERT 第3条: id=3, name=王五");
        }

        // 使用 Statement 批量插入
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO DBA." + TEST_TABLE +
                    " (id, name, email, age, salary, active) VALUES " +
                    "(4, '赵六', 'zhaoliu@example.com', 26, 6800.00, 'T'), " +
                    "(5, '孙七', 'sunqi@example.com', 31, 9500.00, 'T')");
            System.out.println("  ✓ INSERT 批量插入: id=4,5");
        }

        System.out.println("  ✓ INSERT 测试完成，共插入 5 条记录");
    }

    /**
     * SELECT 测试：多种查询方式
     */
    private static void selectTestData(Connection conn) throws SQLException {
        System.out.println("\n  --- 3. SELECT 测试 ---");

        // 查询所有记录
        System.out.println("  [查询所有记录]");
        String sql = "SELECT id, name, email, age, salary, active, create_time " +
                "FROM DBA." + TEST_TABLE + " ORDER BY id";
        printQueryResult(conn, sql);

        // 条件查询：active='T'
        System.out.println("  [条件查询: active='T']");
        String condSql = "SELECT id, name, email, age, salary " +
                "FROM DBA." + TEST_TABLE + " WHERE active = 'T' ORDER BY id";
        printQueryResult(conn, condSql);

        // 聚合查询
        System.out.println("  [聚合查询: COUNT, AVG]");
        String aggSql = "SELECT COUNT(*) AS total, AVG(age) AS avg_age, " +
                "AVG(salary) AS avg_salary FROM DBA." + TEST_TABLE;
        printQueryResult(conn, aggSql);

        // 使用 PreparedStatement 参数化查询
        System.out.println("  [参数化查询: age > ?]");
        String paramSql = "SELECT id, name, age, salary FROM DBA." + TEST_TABLE +
                " WHERE age > ? ORDER BY id";
        try (PreparedStatement pstmt = conn.prepareStatement(paramSql)) {
            pstmt.setInt(1, 30);
            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    System.out.printf("  id=%d, name=%s, age=%d, salary=%.2f%n",
                            rs.getInt("id"), rs.getString("name"),
                            rs.getInt("age"), rs.getBigDecimal("salary"));
                    count++;
                }
                System.out.println("  (共 " + count + " 行)");
            }
        }
    }

    /**
     * UPDATE 测试：更新单条和多条记录
     */
    private static void updateTestData(Connection conn) throws SQLException {
        System.out.println("\n  --- 4. UPDATE 测试 ---");

        // 更新单条记录
        String updateSql = "UPDATE DBA." + TEST_TABLE +
                " SET salary = ?, update_time = CURRENT TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setBigDecimal(1, new java.math.BigDecimal("9900.00"));
            pstmt.setInt(2, 1);
            int affected = pstmt.executeUpdate();
            System.out.println("  ✓ UPDATE 单条: id=1 salary→9900.00, 影响行数=" + affected);
        }

        // 批量更新：将所有 active='T' 的年龄 +1
        String batchUpdateSql = "UPDATE DBA." + TEST_TABLE +
                " SET age = age + 1, update_time = CURRENT TIMESTAMP WHERE active = 'T'";
        try (Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(batchUpdateSql);
            System.out.println("  ✓ UPDATE 批量: active='T' 的记录 age+1, 影响行数=" + affected);
        }

        System.out.println("  ✓ UPDATE 测试完成");
    }

    /**
     * DELETE 测试：删除单条和多条记录
     */
    private static void deleteTestData(Connection conn) throws SQLException {
        System.out.println("\n  --- 5. DELETE 测试 ---");

        // 删除单条记录
        String deleteSql = "DELETE FROM DBA." + TEST_TABLE + " WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, 5);
            int affected = pstmt.executeUpdate();
            System.out.println("  ✓ DELETE 单条: id=5, 影响行数=" + affected);
        }

        // 按条件删除
        String condDeleteSql = "DELETE FROM DBA." + TEST_TABLE + " WHERE active = 'F'";
        try (Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(condDeleteSql);
            System.out.println("  ✓ DELETE 按条件: active='F', 影响行数=" + affected);
        }

        System.out.println("  ✓ DELETE 测试完成");
    }
}