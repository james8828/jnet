package com.nova.bioconnect.schedule.sync;

import java.util.List;

/**
 * 医护人员数据查询策略接口
 * 支持多种数据源：数据库直连、RESTful API、HL7 等
 */
public interface OperatorQueryStrategy {

    /**
     * 查询在院医护人员列表
     */
    List<OperatorData> fetchActiveOperators();

    /**
     * 查询指定时间段内变更的医护人员
     */
    List<OperatorData> fetchChangedOperators(java.time.LocalDateTime since);

    /**
     * 查询单个医护人员
     */
    OperatorData fetchOperator(String operatorId);

    /**
     * 获取策略名称
     */
    String strategyName();
}
