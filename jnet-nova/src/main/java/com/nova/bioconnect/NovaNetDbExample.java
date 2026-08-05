package com.nova.bioconnect;

/**
 * NovaNet Sybase SQL Anywhere 数据库连接示例
 * 
 * 前置条件：
 * 1. 安装 Sybase SQL Anywhere 12 客户端（包含 jconn4.jar JDBC 驱动）
 *    驱动位置示例：C:\Program Files\SQL Anywhere 12\java\jconn4.jar
 * 2. 将 jconn4.jar 加入 classpath
 * 
 * JDBC 驱动类：com.sybase.jdbc4.jdbc.SybDriver
 * JDBC URL 格式：jdbc:sybase:Tds:host:port?ServiceName=xxx
 */

import java.sql.*;

public class NovaNetDbExample {

    // ========== 数据库连接配置 ==========
    
    // 服务器地址（根据实际部署修改）
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 2638;  // SQL Anywhere 默认端口
    
    // 数据库凭据（从 NNDBPF.ENC 解密获得，v3.6.12.14）
    private static final String DBA_USER = "dba";
    private static final String DBA_PASSWORD = "ab3dq@RND";
    
    private static final String BACKEND_USER = "rtmbackend";
    private static final String BACKEND_PASSWORD = "ab3dq@B3S";
    
    // SQL Anywhere ODBC DSN 名（从注册表读取）
    // Runtime -> rtmbackend, History -> history, Strings -> anywhere_strings
    // ProfileTrack -> profile_track, Metrics -> metrics
    
    // OEM 强制签名（必须在首次连接时执行）
    private static final String OEM_AUTH_SQL = 
        "SET TEMPORARY OPTION CONNECTION_AUTHENTICATION=" +
        "'Company=Nova Biomedical;" +
        "Application=NovaNet;" +
        "Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158'";

    public static void main(String[] args) {
        String serviceName = "rtmbackend";  // Runtime 数据库
        String url = buildJdbcUrl(DB_HOST, DB_PORT, serviceName);
        
        System.out.println("=== NovaNet SQL Anywhere 连接示例 ===");
        System.out.println("JDBC URL: " + url);
        System.out.println();
        
        // 示例1：DBA 连接（全权限）
        try (Connection conn = connect(url, DBA_USER, DBA_PASSWORD)) {
            System.out.println("[DBA] 连接成功！");
            runOemAuthentication(conn);
            
            // 示例查询
            queryPatients(conn);
            queryPatientsView(conn);
            queryVersionInfo(conn);
            queryHealthPing(conn);
            
        } catch (SQLException e) {
            System.err.println("[DBA] 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        
        // 示例2：应用后端连接
        try (Connection conn = connect(url, BACKEND_USER, BACKEND_PASSWORD)) {
            System.out.println("[BACKEND] 连接成功！");
            runOemAuthentication(conn);
            
            // 查询就诊记录
            queryVisits(conn);
            
        } catch (SQLException e) {
            System.err.println("[BACKEND] 连接失败: " + e.getMessage());
        }
        
        // 示例3：连接其他数据库
        connectToOtherDatabases();
    }
    
    // ========== 核心方法 ==========
    
    /**
     * 构建 JDBC URL
     */
    private static String buildJdbcUrl(String host, int port, String serviceName) {
        return "jdbc:sybase:Tds:" + host + ":" + port + "?ServiceName=" + serviceName;
    }
    
    /**
     * 建立数据库连接
     */
    private static Connection connect(String url, String user, String password) throws SQLException {
        // 方式1：DriverManager 方式
        // Class.forName("com.sybase.jdbc4.jdbc.SybDriver");  // JDBC 4.0+ 自动加载
        return DriverManager.getConnection(url, user, password);
        
        // 方式2：DataSource 方式（推荐用于生产环境）
        // com.sybase.jdbc4.jdbc.SybDataSource ds = new com.sybase.jdbc4.jdbc.SybDataSource();
        // ds.setServerName(DB_HOST);
        // ds.setPortNumber(DB_PORT);
        // ds.setServiceName(serviceName);
        // ds.setUser(user);
        // ds.setPassword(password);
        // return ds.getConnection();
    }
    
    /**
     * 执行 OEM 认证（必须，否则无法访问数据库）
     */
    private static void runOemAuthentication(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(OEM_AUTH_SQL);
            System.out.println("  OEM 认证成功");
        }
    }
    
    // ========== 查询示例 ==========
    
    /**
     * 查询患者表（DBA.patients）
     */
    private static void queryPatients(Connection conn) throws SQLException {
        System.out.println("\n--- 查询患者表 (DBA.patients) ---");
        String sql = "SELECT TOP 10 patient_id, medrec_num, last_name, first_name, " +
                     "sex, birthdate, race, facil_num, add_date " +
                     "FROM DBA.patients WHERE arch = 'F'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            printResultSet(rs, meta, colCount);
        }
    }
    
    /**
     * 查询患者视图（DBA.patients_view）
     */
    private static void queryPatientsView(Connection conn) throws SQLException {
        System.out.println("\n--- 查询患者视图 (DBA.patients_view) ---");
        String sql = "SELECT TOP 5 Patient_ID, Last_Name, First_Name, Sex, " +
                     "birthdate, race, Address, Home_Phone " +
                     "FROM DBA.patients_view";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            printResultSet(rs, meta, colCount);
        }
    }
    
    /**
     * 查询版本信息
     */
    private static void queryVersionInfo(Connection conn) throws SQLException {
        System.out.println("\n--- 查询版本信息 (DBA.version_info) ---");
        String sql = "SELECT Object_Name, Version FROM DBA.version_info";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            printResultSet(rs, meta, colCount);
        }
    }
    
    /**
     * 查询健康心跳
     */
    private static void queryHealthPing(Connection conn) throws SQLException {
        System.out.println("\n--- 查询进程健康 (DBA.health_ping) ---");
        String sql = "SELECT process_name, host, update_time, do_log, " +
                     "num_messages_processed, tot_messages_processed " +
                     "FROM DBA.health_ping";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            printResultSet(rs, meta, colCount);
        }
    }
    
    /**
     * 查询就诊记录（DBA.patient_visits）
     */
    private static void queryVisits(Connection conn) throws SQLException {
        System.out.println("\n--- 查询就诊记录 (DBA.patient_visits) ---");
        String sql = "SELECT TOP 10 visit_num, patient_uuid, account_uuid, " +
                     "admit_time, discharge_time, loc_num, room_num, bed_num, " +
                     "Attend_Physician, patient_type " +
                     "FROM DBA.patient_visits WHERE arch = 'F'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            printResultSet(rs, meta, colCount);
        }
    }
    
    // ========== 其他数据库连接 ==========
    
    private static void connectToOtherDatabases() {
        String[] databases = {
            "history",        // History 数据库
            "anywhere_strings",  // Strings 数据库
            "profile_track",  // ProfileTrack 数据库
            "metrics"         // Metrics 数据库
        };
        
        for (String db : databases) {
            String url = buildJdbcUrl(DB_HOST, DB_PORT, db);
            try (Connection conn = DriverManager.getConnection(url, DBA_USER, DBA_PASSWORD)) {
                runOemAuthentication(conn);
                System.out.println("[OK] " + db + " - 连接成功");
            } catch (SQLException e) {
                System.out.println("[FAIL] " + db + " - " + e.getMessage());
            }
        }
    }
    
    // ========== 工具方法 ==========
    
    private static void printResultSet(ResultSet rs, ResultSetMetaData meta, int colCount) throws SQLException {
        // 打印列名
        StringBuilder header = new StringBuilder("  ");
        for (int i = 1; i <= colCount; i++) {
            header.append(String.format("%-25s", meta.getColumnLabel(i)));
        }
        System.out.println(header);
        System.out.println("  " + "-".repeat(colCount * 25));
        
        // 打印数据行
        int rowNum = 0;
        while (rs.next()) {
            rowNum++;
            StringBuilder line = new StringBuilder("  ");
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                String strVal = val == null ? "NULL" : val.toString();
                if (strVal.length() > 23) {
                    strVal = strVal.substring(0, 20) + "...";
                }
                line.append(String.format("%-25s", strVal));
            }
            System.out.println(line);
        }
        if (rowNum == 0) {
            System.out.println("  (无数据)");
        } else {
            System.out.println("  共 " + rowNum + " 行");
        }
    }
}
