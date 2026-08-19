package com.nova.bioconnect.schedule.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 定时同步任务配置
 */
@Validated
@ConfigurationProperties(prefix = "bioconnect.sync")
public class SyncProperties {

    /** 全局启用/禁用同步 */
    private boolean enabled = true;

    /** 患者同步配置 */
    private Patient patient = new Patient();

    /** 医护人员同步配置 */
    private Operator operator = new Operator();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    /** 患者同步配置 */
    public static class Patient {
        private boolean enabled = true;

        /** 查询策略：database(数据库直连), rest(RESTful API) */
        @NotBlank
        private String strategy = "database";

        /** 同步间隔（秒） */
        @Positive
        private long intervalSeconds = 60;

        /** 启动时立即执行一次同步 */
        private boolean syncOnStartup = true;

        /** HIS 数据库配置（strategy=database 时使用） */
        private DatabaseConfig database = new DatabaseConfig();

        /** HIS RESTful API 配置（strategy=rest 时使用） */
        private RestConfig rest = new RestConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public long getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(long intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isSyncOnStartup() { return syncOnStartup; }
        public void setSyncOnStartup(boolean syncOnStartup) { this.syncOnStartup = syncOnStartup; }
        public DatabaseConfig getDatabase() { return database; }
        public void setDatabase(DatabaseConfig database) { this.database = database; }
        public RestConfig getRest() { return rest; }
        public void setRest(RestConfig rest) { this.rest = rest; }
    }

    /** 医护人员同步配置 */
    public static class Operator {
        private boolean enabled = true;

        /** 查询策略：database(数据库直连), rest(RESTful API) */
        @NotBlank
        private String strategy = "database";

        /** 同步间隔（秒） */
        @Positive
        private long intervalSeconds = 300;

        /** 启动时立即执行一次同步 */
        private boolean syncOnStartup = true;

        /** HIS 数据库配置（strategy=database 时使用） */
        private DatabaseConfig database = new DatabaseConfig();

        /** HIS RESTful API 配置（strategy=rest 时使用） */
        private RestConfig rest = new RestConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public long getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(long intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isSyncOnStartup() { return syncOnStartup; }
        public void setSyncOnStartup(boolean syncOnStartup) { this.syncOnStartup = syncOnStartup; }
        public DatabaseConfig getDatabase() { return database; }
        public void setDatabase(DatabaseConfig database) { this.database = database; }
        public RestConfig getRest() { return rest; }
        public void setRest(RestConfig rest) { this.rest = rest; }
    }

    /** 数据库配置 */
    public static class DatabaseConfig {
        private String url = "jdbc:sybase:Tds:172.31.0.52:26383?ServiceName=rtmbackend";
        private String username = "dba";
        private String password = "ab3dq@RND";
        private String driverClassName = "com.sybase.jdbc3.jdbc.SybDriver";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }

    /** RESTful API 配置 */
    public static class RestConfig {
        private String baseUrl = "http://his-server:8080/api";
        private String apiKey = "";
        private long connectTimeoutMs = 5000;
        private long readTimeoutMs = 30000;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public long getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
}
