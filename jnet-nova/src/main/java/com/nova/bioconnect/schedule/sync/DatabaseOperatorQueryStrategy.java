package com.nova.bioconnect.schedule.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库查询策略 - 直接从 HIS 数据库查询医护人员数据
 *
 * <p>启用条件：bioconnect.sync.operator.strategy=database
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bioconnect.sync.operator.strategy", havingValue = "database", matchIfMissing = false)
public class DatabaseOperatorQueryStrategy implements OperatorQueryStrategy {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseOperatorQueryStrategy(@Qualifier("hisJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OperatorData> fetchActiveOperators() {
        log.info("Database strategy: fetching active operators...");
        String sql = """
                SELECT
                    operator_id,
                    first_name || ' ' || last_name AS operator_name,
                    first_name,
                    last_name,
                    department,
                    location,
                    loc_num,
                    title,
                    privilege_level,
                    is_supervisor,
                    email,
                    phone,
                    status,
                    facility,
                    effective_start_dttm,
                    effective_end_dttm,
                    last_update_time
                FROM DBA.operators
                WHERE status = 'A'
                  AND (effective_end_dttm IS NULL OR effective_end_dttm > CURRENT TIMESTAMP)
                """;

        try {
            return jdbcTemplate.query(sql, this::mapOperatorData);
        } catch (Exception e) {
            log.error("Failed to fetch active operators from database: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<OperatorData> fetchChangedOperators(LocalDateTime since) {
        log.info("Database strategy: fetching changed operators since {}...", since);
        String sql = """
                SELECT
                    operator_id,
                    first_name || ' ' || last_name AS operator_name,
                    first_name,
                    last_name,
                    department,
                    location,
                    loc_num,
                    title,
                    privilege_level,
                    is_supervisor,
                    email,
                    phone,
                    status,
                    facility,
                    effective_start_dttm,
                    effective_end_dttm,
                    last_update_time
                FROM DBA.operators
                WHERE last_update_time >= ?
                """;

        try {
            return jdbcTemplate.query(sql, this::mapOperatorData, Timestamp.valueOf(since));
        } catch (Exception e) {
            log.error("Failed to fetch changed operators from database: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public OperatorData fetchOperator(String operatorId) {
        log.info("Database strategy: fetching operator id={}", operatorId);
        String sql = """
                SELECT
                    operator_id,
                    first_name || ' ' || last_name AS operator_name,
                    first_name,
                    last_name,
                    department,
                    location,
                    loc_num,
                    title,
                    privilege_level,
                    is_supervisor,
                    email,
                    phone,
                    status,
                    facility,
                    effective_start_dttm,
                    effective_end_dttm,
                    last_update_time
                FROM DBA.operators
                WHERE operator_id = ?
                """;

        try {
            List<OperatorData> results = jdbcTemplate.query(sql, this::mapOperatorData, operatorId);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("Failed to fetch operator from database: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String strategyName() {
        return "database";
    }

    private OperatorData mapOperatorData(ResultSet rs, int rowNum) throws SQLException {
        return new OperatorData(
                rs.getString("operator_id"),
                rs.getString("operator_name"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department"),
                rs.getString("location"),
                rs.getString("loc_num"),
                rs.getString("title"),
                rs.getString("privilege_level"),
                "T".equals(rs.getString("is_supervisor")),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("status"),
                rs.getString("facility"),
                toLocalDateTime(rs.getTimestamp("effective_start_dttm")),
                toLocalDateTime(rs.getTimestamp("effective_end_dttm")),
                toLocalDateTime(rs.getTimestamp("last_update_time"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
