package com.nova.bioconnect.schedule.sync;

import com.nova.bioconnect.rtm.entity.OperatorEntity;
import com.nova.bioconnect.rtm.repository.OperatorRepository;
import com.nova.bioconnect.rtm.service.OperatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 医护人员同步服务 - 对比外部数据与本地数据，识别变更并执行更新
 *
 * <p>变更类型：
 * <ul>
 *   <li>NEW - 新增医护人员</li>
 *   <li>UPDATE - 医护人员信息变更</li>
 *   <li>TRANSFER - 调科/调岗</li>
 *   <li>LEAVE - 离职</li>
 *   <li>RETURN - 返岗</li>
 * </ul>
 */
@Slf4j
@Service
public class OperatorSyncService {

    private final OperatorQueryStrategy queryStrategy;
    private final OperatorRepository operatorRepository;
    private final OperatorService operatorService;

    public OperatorSyncService(OperatorQueryStrategy queryStrategy,
                                OperatorRepository operatorRepository,
                                OperatorService operatorService) {
        this.queryStrategy = queryStrategy;
        this.operatorRepository = operatorRepository;
        this.operatorService = operatorService;
    }

    /**
     * 同步医护人员数据
     */
    @Transactional
    public SyncResult syncAllOperators() {
        log.info("=== Starting operator sync (strategy: {}) ===", queryStrategy.strategyName());

        List<OperatorData> externalOperators = queryStrategy.fetchActiveOperators();
        log.info("Fetched {} operators from HIS", externalOperators.size());

        Map<String, OperatorEntity> localOperators = operatorRepository.findAll().stream()
                .collect(Collectors.toMap(
                        OperatorEntity::getOperatorId,
                        o -> o,
                        (a, b) -> a
                ));
        log.info("Found {} operators in local database", localOperators.size());

        SyncResult result = new SyncResult();

        for (OperatorData ext : externalOperators) {
            ChangeType changeType = detectChange(ext, localOperators);
            if (changeType != ChangeType.NONE) {
                applyChange(ext, changeType);
                result.addChange(changeType, ext.operatorId());
            }
        }

        // 检查本地已离职的医护人员（HIS 不再返回）
        Set<String> extIdSet = externalOperators.stream()
                .map(OperatorData::operatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Map.Entry<String, OperatorEntity> entry : localOperators.entrySet()) {
            if (!extIdSet.contains(entry.getKey()) && "A".equals(entry.getValue().getStatus())) {
                log.info("Operator {} not found in HIS, marking as inactive", entry.getKey());
                operatorService.deleteOperator(entry.getValue().getOperatorNum());
                result.addChange(ChangeType.LEAVE, entry.getKey());
            }
        }

        log.info("=== Operator sync complete: {} ===", result.summary());
        return result;
    }

    /**
     * 增量同步 - 仅同步变更的医护人员
     */
    @Transactional
    public SyncResult syncChangedOperators(LocalDateTime since) {
        log.info("=== Starting incremental operator sync since {} ===", since);

        List<OperatorData> externalOperators = queryStrategy.fetchChangedOperators(since);
        log.info("Fetched {} changed operators from HIS", externalOperators.size());

        SyncResult result = new SyncResult();

        for (OperatorData ext : externalOperators) {
            OperatorEntity local = operatorRepository.findByOperatorId(ext.operatorId()).orElse(null);
            ChangeType changeType = detectChange(ext, local);
            if (changeType != ChangeType.NONE) {
                applyChange(ext, changeType);
                result.addChange(changeType, ext.operatorId());
            }
        }

        log.info("=== Incremental operator sync complete: {} ===", result.summary());
        return result;
    }

    /**
     * 检测变更类型
     */
    private ChangeType detectChange(OperatorData ext, Map<String, OperatorEntity> localMap) {
        OperatorEntity local = localMap.get(ext.operatorId());
        return detectChange(ext, local);
    }

    /**
     * 检测变更类型（单个）
     */
    private ChangeType detectChange(OperatorData ext, OperatorEntity local) {
        if (local == null) {
            return ChangeType.NEW;
        }

        // 检查离职
        if ("I".equals(ext.status()) && "A".equals(local.getStatus())) {
            return ChangeType.LEAVE;
        }

        // 检查返岗
        if ("A".equals(ext.status()) && "I".equals(local.getStatus())) {
            return ChangeType.RETURN;
        }

        // 检查调科
        if (!Objects.equals(ext.department(), local.getDepartment())
                || !Objects.equals(ext.location(), local.getLocation())) {
            return ChangeType.TRANSFER;
        }

        // 检查信息变更
        if (isInfoChanged(ext, local)) {
            return ChangeType.UPDATE;
        }

        return ChangeType.NONE;
    }

    /**
     * 检查信息是否变更
     */
    private boolean isInfoChanged(OperatorData ext, OperatorEntity local) {
        return !Objects.equals(ext.firstName(), local.getFirstName())
                || !Objects.equals(ext.lastName(), local.getLastName())
                || !Objects.equals(ext.email(), local.getEmail())
                || !Objects.equals(ext.isSupervisor(), "T".equals(local.getIsSupervisor()))
                || !Objects.equals(ext.privilegeLevel(), local.getPrivilegeLevel() != null ? local.getPrivilegeLevel().toString() : null);
    }

    /**
     * 应用变更
     */
    private void applyChange(OperatorData ext, ChangeType changeType) {
        log.info("Processing {} for operator: {}", changeType, ext.operatorId());

        switch (changeType) {
            case NEW -> createAndPush(ext);
            case UPDATE -> updateAndPush(ext);
            case TRANSFER -> transferAndPush(ext);
            case LEAVE -> leave(ext);
            case RETURN -> returnToWork(ext);
        }
    }

    /**
     * 新增医护人员
     */
    private void createAndPush(OperatorData ext) {
        OperatorService.OperatorCreateRequest request = new OperatorService.OperatorCreateRequest(
                ext.operatorId(),
                ext.firstName(),
                ext.lastName(),
                ext.isSupervisor(),
                ext.privilegeLevel() != null ? Integer.parseInt(ext.privilegeLevel()) : 2,
                null,  // privileges
                ext.location() != null ? List.of(ext.location()) : null,  // unitLocNums
                null   // methods
        );
        operatorService.createOperator(request);
    }

    /**
     * 更新医护人员信息
     */
    private void updateAndPush(OperatorData ext) {
        Optional<OperatorEntity> existing = operatorRepository.findByOperatorId(ext.operatorId());
        if (existing.isPresent()) {
            OperatorService.OperatorUpdateRequest request = new OperatorService.OperatorUpdateRequest(
                    ext.firstName(),
                    ext.lastName(),
                    ext.isSupervisor(),
                    ext.privilegeLevel() != null ? Integer.parseInt(ext.privilegeLevel()) : null,
                    null  // privileges
            );
            operatorService.updateOperator(existing.get().getOperatorNum(), request);
        }
    }

    /**
     * 调科/调岗
     */
    private void transferAndPush(OperatorData ext) {
        // 调科/调岗的逻辑与 UPDATE 类似，但需要额外处理位置关联
        updateAndPush(ext);
        log.info("Operator {} transferred to department={}, location={}",
                ext.operatorId(), ext.department(), ext.location());
    }

    /**
     * 离职
     */
    private void leave(OperatorData ext) {
        Optional<OperatorEntity> existing = operatorRepository.findByOperatorId(ext.operatorId());
        if (existing.isPresent()) {
            operatorService.deleteOperator(existing.get().getOperatorNum());
        }
    }

    /**
     * 返岗
     */
    private void returnToWork(OperatorData ext) {
        // 返岗：重新创建或更新为活跃状态
        createAndPush(ext);
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        NEW,      // 新增
        UPDATE,   // 信息更新
        TRANSFER, // 调科/调岗
        LEAVE,    // 离职
        RETURN,   // 返岗
        NONE      // 无变更
    }

    /**
     * 同步结果
     */
    public record SyncResult(
            int newCount,
            int updateCount,
            int transferCount,
            int leaveCount,
            int returnCount,
            int totalChanges
    ) {
        public SyncResult() {
            this(0, 0, 0, 0, 0, 0);
        }

        public void addChange(ChangeType type, String operatorId) {
            switch (type) {
                case NEW -> increment(0);
                case UPDATE -> increment(1);
                case TRANSFER -> increment(2);
                case LEAVE -> increment(3);
                case RETURN -> increment(4);
                case NONE -> {}
            }
        }

        private void increment(int index) {
            int[] counts = {newCount, updateCount, transferCount, leaveCount, returnCount};
            counts[index]++;
        }

        public String summary() {
            return String.format("new=%d, update=%d, transfer=%d, leave=%d, return=%d, total=%d",
                    newCount, updateCount, transferCount, leaveCount, returnCount, totalChanges);
        }
    }
}
