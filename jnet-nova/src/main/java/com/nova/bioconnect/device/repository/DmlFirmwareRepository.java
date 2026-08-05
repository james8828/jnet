package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlFirmware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Firmware Repository
 */
@Repository
public interface DmlFirmwareRepository extends JpaRepository<DmlFirmware, Long> {

    List<DmlFirmware> findByDeviceTypeAndStatus(String deviceType, String status);

    @Query("SELECT f FROM DmlFirmware f WHERE f.deviceType = :deviceType AND f.status = 'Active' ORDER BY f.releaseDate DESC")
    List<DmlFirmware> findLatestFirmware(@Param("deviceType") String deviceType);

    @Query("SELECT f FROM DmlFirmware f WHERE f.deviceClass = :deviceClass AND f.status = 'Active' ORDER BY f.releaseDate DESC")
    List<DmlFirmware> findLatestFirmwareByClass(@Param("deviceClass") String deviceClass);
}
