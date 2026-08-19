package com.nova.bioconnect.schedule.sync;

import java.time.LocalDateTime;

/**
 * 患者外部数据记录（从 HIS/LIS 查询获取）
 */
public record PatientData(
        String patientId,           // 患者ID
        String medrecNum,           // 病历号 MRN
        String patientName,         // 患者姓名
        String firstName,           // 名
        String lastName,            // 姓
        String sex,                 // 性别
        LocalDateTime birthDate,    // 出生日期
        String visitNum,            // 就诊号
        String accountNum,          // 账户号
        String visitType,           // 就诊类型（门诊/住院/急诊）
        String location,            // 科室
        String room,                // 房间
        String bed,                 // 床位
        String attendingDoctor,     // 主治医师
        LocalDateTime admitTime,    // 入院时间
        LocalDateTime dischargeTime, // 出院时间
        String status,              // 状态（A=在院, D=出院, T=转科）
        String facility,            // 机构
        String idCard,              // 身份证号
        String phone,               // 电话
        LocalDateTime lastUpdateTime // 最后更新时间
) {
}
