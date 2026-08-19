package com.nova.bioconnect.schedule.sync;

import java.util.List;

/**
 * 患者数据查询策略接口
 * 支持多种数据源：数据库直连、RESTful API、HL7 等
 */
public interface PatientQueryStrategy {

    /**
     * 查询在院患者列表
     */
    List<PatientData> fetchActivePatients();

    /**
     * 查询指定时间段内变更的患者
     */
    List<PatientData> fetchChangedPatients(java.time.LocalDateTime since);

    /**
     * 查询单个患者
     */
    PatientData fetchPatient(String patientId, String medrecNum);

    /**
     * 获取策略名称
     */
    String strategyName();
}
