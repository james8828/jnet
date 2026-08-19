package com.nova.bioconnect.schedule;

import com.nova.bioconnect.schedule.config.SyncProperties;
import com.nova.bioconnect.schedule.sync.OperatorSyncService;
import com.nova.bioconnect.schedule.sync.PatientSyncService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 数据同步定时调度器
 *
 * <p>调度 HIS → Java 的患者和医护人员数据同步任务。
 *
 * <p>配置示例：
 * <pre>
 * bioconnect:
 *   sync:
 *     patient:
 *       enabled: true
 *       strategy: database  # database | rest
 *       interval-seconds: 60
 *       sync-on-startup: true
 *     operator:
 *       enabled: true
 *       strategy: database
 *       interval-seconds: 300
 *       sync-on-startup: true
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bioconnect.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private final SyncProperties properties;
    private final PatientSyncService patientSyncService;
    private final OperatorSyncService operatorSyncService;

    private ScheduledExecutorService executorService;
    private volatile LocalDateTime lastPatientSyncTime;
    private volatile LocalDateTime lastOperatorSyncTime;

    public SyncScheduler(SyncProperties properties,
                          PatientSyncService patientSyncService,
                          OperatorSyncService operatorSyncService) {
        this.properties = properties;
        this.patientSyncService = patientSyncService;
        this.operatorSyncService = operatorSyncService;
    }

    /**
     * 启动时执行一次全量同步
     */
    @PostConstruct
    public void onStartup() {
        executorService = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "sync-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 启动时立即同步
        if (properties.getPatient().isEnabled() && properties.getPatient().isSyncOnStartup()) {
            executorService.submit(this::syncPatientsSafely);
        }
        if (properties.getOperator().isEnabled() && properties.getOperator().isSyncOnStartup()) {
            executorService.submit(this::syncOperatorsSafely);
        }

        log.info("Sync scheduler initialized: patient(interval={}s, strategy={}), operator(interval={}s, strategy={})",
                properties.getPatient().getIntervalSeconds(), properties.getPatient().getStrategy(),
                properties.getOperator().getIntervalSeconds(), properties.getOperator().getStrategy());
    }

    /**
     * 关闭时清理
     */
    @PreDestroy
    public void onShutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("Sync scheduler shutdown");
    }

    /**
     * 患者定时同步
     */
    @Scheduled(fixedDelayString = "${bioconnect.sync.patient.interval-seconds:60}000")
    public void syncPatients() {
        if (!properties.getPatient().isEnabled()) {
            return;
        }
        syncPatientsSafely();
    }

    /**
     * 医护人员定时同步
     */
    @Scheduled(fixedDelayString = "${bioconnect.sync.operator.interval-seconds:300}000")
    public void syncOperators() {
        if (!properties.getOperator().isEnabled()) {
            return;
        }
        syncOperatorsSafely();
    }

    /**
     * 安全的患者同步（捕获异常）
     */
    private void syncPatientsSafely() {
        try {
            log.info("=== Patient sync started ===");
            LocalDateTime now = LocalDateTime.now();

            // 全量同步（首次）或增量同步
            PatientSyncService.SyncResult result;
            if (lastPatientSyncTime == null) {
                result = patientSyncService.syncAllPatients();
            } else {
                result = patientSyncService.syncChangedPatients(lastPatientSyncTime);
            }

            lastPatientSyncTime = now;
            log.info("=== Patient sync finished: {} ===", result.summary());
        } catch (Exception e) {
            log.error("Patient sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 安全的医护人员同步（捕获异常）
     */
    private void syncOperatorsSafely() {
        try {
            log.info("=== Operator sync started ===");
            LocalDateTime now = LocalDateTime.now();

            OperatorSyncService.SyncResult result;
            if (lastOperatorSyncTime == null) {
                result = operatorSyncService.syncAllOperators();
            } else {
                result = operatorSyncService.syncChangedOperators(lastOperatorSyncTime);
            }

            lastOperatorSyncTime = now;
            log.info("=== Operator sync finished: {} ===", result.summary());
        } catch (Exception e) {
            log.error("Operator sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发患者同步
     */
    public PatientSyncService.SyncResult triggerPatientSync() {
        return patientSyncService.syncAllPatients();
    }

    /**
     * 手动触发医护人员同步
     */
    public OperatorSyncService.SyncResult triggerOperatorSync() {
        return operatorSyncService.syncAllOperators();
    }

    /**
     * 获取同步状态
     */
    public String getStatus() {
        return String.format("Patient: last=%s, enabled=%s, interval=%ds | Operator: last=%s, enabled=%s, interval=%ds",
                lastPatientSyncTime,
                properties.getPatient().isEnabled(),
                properties.getPatient().getIntervalSeconds(),
                lastOperatorSyncTime,
                properties.getOperator().isEnabled(),
                properties.getOperator().getIntervalSeconds());
    }
}
