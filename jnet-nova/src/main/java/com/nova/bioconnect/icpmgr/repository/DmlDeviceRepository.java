package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DML Device Repository
 */
@Repository
public interface DmlDeviceRepository extends JpaRepository<DmlDevice, Long> {

    /**
     * Find device by serial ID
     */
    Optional<DmlDevice> findBySerialId(String serialId);

    /**
     * Find all devices by device type
     */
    List<DmlDevice> findByDeviceType(String deviceType);

    /**
     * Find devices updated after a specific time
     */
    List<DmlDevice> findByUpdatedAtAfter(LocalDateTime after);

    /**
     * Check if device exists by serial ID
     */
    boolean existsBySerialId(String serialId);

    /**
     * Delete device by serial ID
     */
    void deleteBySerialId(String serialId);
}