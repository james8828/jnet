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
}
