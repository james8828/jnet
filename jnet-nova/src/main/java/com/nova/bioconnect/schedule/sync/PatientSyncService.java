package com.nova.bioconnect.schedule.sync;

import com.nova.bioconnect.rtm.entity.PatientEntity;
import com.nova.bioconnect.rtm.model.PatientInfo;
import com.nova.bioconnect.rtm.model.VisitInfo;
import com.nova.bioconnect.rtm.repository.PatientRepository;
import com.nova.bioconnect.rtm.service.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 患者同步服务 - 对比外部数据与本地数据，识别变更并执行更新
 *
 * <p>变更类型：
 * <ul>
 *   <li>NEW - 新增患者</li>
 *   <li>ADMIT - 入院（新增就诊记录）</li>
 *   <li>UPDATE - 患者信息变更（姓名、性别等）</li>
 *   <li>TRANSFER - 转科/转床</li>
 *   <li>DISCHARGE - 出院</li>
 * </ul>
 */
@Slf4j
@Service
public class PatientSyncService {

    private final PatientQueryStrategy queryStrategy;
    private final PatientRepository patientRepository;
    private final PatientService patientService;

    public PatientSyncService(PatientQueryStrategy queryStrategy,
                               PatientRepository patientRepository,
                               PatientService patientService) {
        this.queryStrategy = queryStrategy;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
    }

    /**
     * 同步患者数据
     */
    @Transactional
    public SyncResult syncAllPatients() {
        log.info("=== Starting patient sync (strategy: {}) ===", queryStrategy.strategyName());

        List<PatientData> externalPatients = queryStrategy.fetchActivePatients();
        log.info("Fetched {} patients from HIS", externalPatients.size());

        Map<String, PatientEntity> localPatients = patientRepository.findAll().stream()
                .collect(Collectors.toMap(
                        PatientEntity::getMedrecNum,
                        p -> p,
                        (a, b) -> a  // 如果有重复，保留第一个
                ));
        log.info("Found {} patients in local database", localPatients.size());

        SyncResult result = new SyncResult();

        for (PatientData ext : externalPatients) {
            ChangeType changeType = detectChange(ext, localPatients);
            if (changeType != ChangeType.NONE) {
                applyChange(ext, changeType);
                result.addChange(changeType, ext.patientId());
            }
        }

        // 检查本地已出院的患者（HIS 不再返回）
        Set<String> extMrnSet = externalPatients.stream()
                .map(PatientData::medrecNum)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Map.Entry<String, PatientEntity> entry : localPatients.entrySet()) {
            if (!extMrnSet.contains(entry.getKey()) && "A".equals(entry.getValue().getStatus())) {
                // 患者在本地是活跃的，但 HIS 不再返回，可能已出院
                // 可以选择标记为 I (inactive)
                log.debug("Patient {} not found in HIS, marking as inactive", entry.getKey());
            }
        }

        log.info("=== Patient sync complete: {} ===", result.summary());
        return result;
    }

    /**
     * 增量同步 - 仅同步变更的患者
     */
    @Transactional
    public SyncResult syncChangedPatients(LocalDateTime since) {
        log.info("=== Starting incremental patient sync since {} ===", since);

        List<PatientData> externalPatients = queryStrategy.fetchChangedPatients(since);
        log.info("Fetched {} changed patients from HIS", externalPatients.size());

        SyncResult result = new SyncResult();
        Map<String, PatientEntity> localCache = new HashMap<>();

        for (PatientData ext : externalPatients) {
            PatientEntity local = null;
            if (ext.medrecNum() != null) {
                local = patientRepository.findByMedrecNum(ext.medrecNum()).orElse(null);
            }
            if (local == null && ext.patientId() != null) {
                local = patientRepository.findByPatientId(ext.patientId()).orElse(null);
            }

            ChangeType changeType = detectChange(ext, local);
            if (changeType != ChangeType.NONE) {
                applyChange(ext, changeType);
                result.addChange(changeType, ext.patientId());
            }
        }

        log.info("=== Incremental patient sync complete: {} ===", result.summary());
        return result;
    }

    /**
     * 检测变更类型
     */
    private ChangeType detectChange(PatientData ext, Map<String, PatientEntity> localMap) {
        PatientEntity local = null;
        if (ext.medrecNum() != null) {
            local = localMap.get(ext.medrecNum());
        }
        if (local == null && ext.patientId() != null) {
            // 按 patientId 查找
            local = localMap.values().stream()
                    .filter(p -> ext.patientId().equals(p.getPatientId()))
                    .findFirst().orElse(null);
        }

        return detectChange(ext, local);
    }

    /**
     * 检测变更类型（单个患者）
     */
    private ChangeType detectChange(PatientData ext, PatientEntity local) {
        if (local == null) {
            // 新患者
            return ChangeType.NEW;
        }

        // 检查出院
        if ("D".equals(ext.status()) && !"D".equals(local.getStatus())) {
            return ChangeType.DISCHARGE;
        }

        // 检查入院（状态从 I 变为 A）
        if ("A".equals(ext.status()) && "I".equals(local.getStatus())) {
            return ChangeType.ADMIT;
        }

        // 检查转科
        if (ext.location() != null && !ext.location().equals(local.getLocation())) {
            return ChangeType.TRANSFER;
        }

        // 检查基本信息变更
        if (isBasicInfoChanged(ext, local)) {
            return ChangeType.UPDATE;
        }

        return ChangeType.NONE;
    }

    /**
     * 检查基本信息是否变更
     */
    private boolean isBasicInfoChanged(PatientData ext, PatientEntity local) {
        return !Objects.equals(ext.firstName(), local.getFirstName())
                || !Objects.equals(ext.lastName(), local.getLastName())
                || !Objects.equals(ext.sex(), local.getSex())
                || !Objects.equals(ext.birthDate(), local.getBirthDate())
                || !Objects.equals(ext.phone(), local.getPhoneHome())
                || !Objects.equals(ext.visitNum(), local.getAccountNum());
    }

    /**
     * 应用变更
     */
    private void applyChange(PatientData ext, ChangeType changeType) {
        log.info("Processing {} for patient: {}", changeType, ext.patientId());

        switch (changeType) {
            case NEW -> createAndPush(ext);
            case ADMIT -> admitAndPush(ext);
            case UPDATE -> updateAndPush(ext);
            case TRANSFER -> transferAndPush(ext);
            case DISCHARGE -> dischargeAndPush(ext);
        }
    }

    /**
     * 新增患者
     */
    private void createAndPush(PatientData ext) {
        PatientInfo patientInfo = toPatientInfo(ext);
        VisitInfo visitInfo = toVisitInfo(ext);
        patientService.processAdtEvent("A01", patientInfo, visitInfo, null);
    }

    /**
     * 入院
     */
    private void admitAndPush(PatientData ext) {
        PatientInfo patientInfo = toPatientInfo(ext);
        VisitInfo visitInfo = toVisitInfo(ext);
        patientService.processAdtEvent("A04", patientInfo, visitInfo, null);
    }

    /**
     * 更新患者信息
     */
    private void updateAndPush(PatientData ext) {
        PatientInfo patientInfo = toPatientInfo(ext);
        VisitInfo visitInfo = toVisitInfo(ext);
        patientService.processAdtEvent("A08", patientInfo, visitInfo, null);
    }

    /**
     * 转科
     */
    private void transferAndPush(PatientData ext) {
        PatientInfo patientInfo = toPatientInfo(ext);
        VisitInfo visitInfo = toVisitInfo(ext);
        patientService.processAdtEvent("A02", patientInfo, visitInfo, null);
    }

    /**
     * 出院
     */
    private void dischargeAndPush(PatientData ext) {
        PatientInfo patientInfo = toPatientInfo(ext);
        VisitInfo visitInfo = toVisitInfo(ext);
        patientService.processAdtEvent("A03", patientInfo, visitInfo, null);
    }

    /**
     * 转换外部数据为 PatientInfo
     */
    private PatientInfo toPatientInfo(PatientData ext) {
        return new PatientInfo(
                ext.patientId(),           // externalPatientId
                ext.medrecNum(),           // internalPatientId
                ext.lastName(),            // lastName
                ext.firstName(),           // firstName
                null,                      // middleName
                null,                      // prefix
                null,                      // suffix
                ext.birthDate() != null ? ext.birthDate().toLocalDate() : null, // dateOfBirth
                ext.sex(),                 // gender
                null,                      // race
                null,                      // address
                ext.phone(),               // phoneHome
                ext.accountNum()           // accountNumber
        );
    }

    /**
     * 转换外部数据为 VisitInfo
     */
    private VisitInfo toVisitInfo(PatientData ext) {
        return new VisitInfo(
                ext.visitType(),           // patientClass
                ext.location(),            // assignedLocation
                ext.room(),                // room
                ext.bed(),                 // bed
                ext.facility(),            // facility
                null,                      // priorPatientLocation
                ext.attendingDoctor(),     // attendingPhysician
                null,                      // hospitalService
                null,                      // patientType
                ext.visitNum(),            // visitNumber
                ext.admitTime(),           // admitDateTime
                ext.dischargeTime()        // dischargeDateTime
        );
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        NEW,        // 新增患者
        ADMIT,      // 入院
        UPDATE,     // 信息更新
        TRANSFER,   // 转科
        DISCHARGE,  // 出院
        NONE        // 无变更
    }

    /**
     * 同步结果
     */
    public record SyncResult(
            int newCount,
            int admitCount,
            int updateCount,
            int transferCount,
            int dischargeCount,
            int totalChanges
    ) {
        public SyncResult() {
            this(0, 0, 0, 0, 0, 0);
        }

        public void addChange(ChangeType type, String patientId) {
            switch (type) {
                case NEW -> increment(0);
                case ADMIT -> increment(1);
                case UPDATE -> increment(2);
                case TRANSFER -> increment(3);
                case DISCHARGE -> increment(4);
                case NONE -> {}
            }
        }

        private void increment(int index) {
            int[] counts = {newCount, admitCount, updateCount, transferCount, dischargeCount};
            counts[index]++;
        }

        public String summary() {
            return String.format("new=%d, admit=%d, update=%d, transfer=%d, discharge=%d, total=%d",
                    newCount, admitCount, updateCount, transferCount, dischargeCount, totalChanges);
        }
    }
}
