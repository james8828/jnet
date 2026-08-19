package com.nova.bioconnect.schedule.sync;

import java.time.LocalDateTime;

/**
 * 医护人员外部数据记录（从 HIS/LIS 查询获取）
 */
public record OperatorData(
        String operatorId,          // 医护人员ID
        String operatorName,        // 医护人员姓名
        String firstName,           // 名
        String lastName,            // 姓
        String department,          // 科室
        String location,            // 位置
        String locNum,              // 位置编号
        String title,               // 职称
        String privilegeLevel,      // 权限级别
        boolean isSupervisor,       // 是否主管
        String email,               // 邮箱
        String phone,               // 电话
        String status,              // 状态（A=在职, I=离职）
        String facility,            // 机构
        LocalDateTime effectiveStart, // 生效开始时间
        LocalDateTime effectiveEnd,   // 生效结束时间
        LocalDateTime lastUpdateTime  // 最后更新时间
) {
}
