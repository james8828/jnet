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
 * 数据库查询策略 - 直接从 HIS 数据库查询患者数据
 *
 * <p>启用条件：bioconnect.sync.patient.strategy=database
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bioconnect.sync.patient.strategy", havingValue = "database", matchIfMissing = false)
public class DatabasePatientQueryStrategy implements PatientQueryStrategy {

    private final JdbcTemplate jdbcTemplate;

    public DatabasePatientQueryStrategy(@Qualifier("hisJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PatientData> fetchActivePatients() {
        log.info("Database strategy: fetching active patients...");
        String sql = """
                SELECT
                    p.patient_id,
                    p.medrec_num,
                    p.last_name || ' ' || p.first_name AS patient_name,
                    p.first_name,
                    p.last_name,
                    p.sex,
                    p.birthdate,
                    pv.visit_num,
                    pv.account_uuid AS account_num,
                    pv.patient_type AS visit_type,
                    pv.loc_num AS location,
                    pv.room_num AS room,
                    pv.bed_num AS bed,
                    pv.Attend_Physician AS attending_doctor,
                    pv.admit_time,
                    pv.discharge_time,
                    CASE
                        WHEN pv.discharge_time IS NOT NULL THEN 'D'
                        WHEN pv.admit_time IS NOT NULL THEN 'A'
                        ELSE 'T'
                    END AS status,
                    p.facil_num AS facility,
                    p.patient_id AS id_card,
                    cp.cell_phone AS phone,
                    pv.last_update_date AS last_update_time
                FROM DBA.patients p
                LEFT JOIN DBA.patient_visits pv ON p.patient_id = pv.patient_id AND pv.arch = 'F'
                LEFT JOIN DBA.contact_info cp ON p.patient_id = cp.patient_id
                WHERE p.arch = 'F'
                  AND pv.admit_time IS NOT NULL
                """;

        try {
            return jdbcTemplate.query(sql, this::mapPatientData);
        } catch (Exception e) {
            log.error("Failed to fetch active patients from database: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<PatientData> fetchChangedPatients(LocalDateTime since) {
        log.info("Database strategy: fetching changed patients since {}...", since);
        String sql = """
                SELECT
                    p.patient_id,
                    p.medrec_num,
                    p.last_name || ' ' || p.first_name AS patient_name,
                    p.first_name,
                    p.last_name,
                    p.sex,
                    p.birthdate,
                    pv.visit_num,
                    pv.account_uuid AS account_num,
                    pv.patient_type AS visit_type,
                    pv.loc_num AS location,
                    pv.room_num AS room,
                    pv.bed_num AS bed,
                    pv.Attend_Physician AS attending_doctor,
                    pv.admit_time,
                    pv.discharge_time,
                    CASE
                        WHEN pv.discharge_time IS NOT NULL THEN 'D'
                        WHEN pv.admit_time IS NOT NULL THEN 'A'
                        ELSE 'T'
                    END AS status,
                    p.facil_num AS facility,
                    p.patient_id AS id_card,
                    cp.cell_phone AS phone,
                    COALESCE(pv.last_update_date, p.add_date) AS last_update_time
                FROM DBA.patients p
                LEFT JOIN DBA.patient_visits pv ON p.patient_id = pv.patient_id
                LEFT JOIN DBA.contact_info cp ON p.patient_id = cp.patient_id
                WHERE (p.add_date >= ? OR pv.last_update_date >= ?)
                """;

        try {
            return jdbcTemplate.query(sql, this::mapPatientData, Timestamp.valueOf(since), Timestamp.valueOf(since));
        } catch (Exception e) {
            log.error("Failed to fetch changed patients from database: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public PatientData fetchPatient(String patientId, String medrecNum) {
        log.info("Database strategy: fetching patient id={}, medrec={}", patientId, medrecNum);
        String sql = """
                SELECT
                    p.patient_id,
                    p.medrec_num,
                    p.last_name || ' ' || p.first_name AS patient_name,
                    p.first_name,
                    p.last_name,
                    p.sex,
                    p.birthdate,
                    pv.visit_num,
                    pv.account_uuid AS account_num,
                    pv.patient_type AS visit_type,
                    pv.loc_num AS location,
                    pv.room_num AS room,
                    pv.bed_num AS bed,
                    pv.Attend_Physician AS attending_doctor,
                    pv.admit_time,
                    pv.discharge_time,
                    CASE
                        WHEN pv.discharge_time IS NOT NULL THEN 'D'
                        WHEN pv.admit_time IS NOT NULL THEN 'A'
                        ELSE 'T'
                    END AS status,
                    p.facil_num AS facility,
                    p.patient_id AS id_card,
                    cp.cell_phone AS phone,
                    pv.last_update_date AS last_update_time
                FROM DBA.patients p
                LEFT JOIN DBA.patient_visits pv ON p.patient_id = pv.patient_id
                LEFT JOIN DBA.contact_info cp ON p.patient_id = cp.patient_id
                WHERE p.patient_id = ? OR p.medrec_num = ?
                """;

        try {
            List<PatientData> results = jdbcTemplate.query(sql, this::mapPatientData, patientId, medrecNum);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("Failed to fetch patient from database: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String strategyName() {
        return "database";
    }

    private PatientData mapPatientData(ResultSet rs, int rowNum) throws SQLException {
        return new PatientData(
                rs.getString("patient_id"),
                rs.getString("medrec_num"),
                rs.getString("patient_name"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("sex"),
                toLocalDateTime(rs.getTimestamp("birthdate")),
                rs.getString("visit_num"),
                rs.getString("account_num"),
                rs.getString("visit_type"),
                rs.getString("location"),
                rs.getString("room"),
                rs.getString("bed"),
                rs.getString("attending_doctor"),
                toLocalDateTime(rs.getTimestamp("admit_time")),
                toLocalDateTime(rs.getTimestamp("discharge_time")),
                rs.getString("status"),
                rs.getString("facility"),
                rs.getString("id_card"),
                rs.getString("phone"),
                toLocalDateTime(rs.getTimestamp("last_update_time"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
