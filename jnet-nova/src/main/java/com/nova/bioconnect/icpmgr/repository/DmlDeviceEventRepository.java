package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlDeviceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DML Device Event Repository
 * Mirrors C# DBA.device_events queries
 */
@Repository
public interface DmlDeviceEventRepository extends JpaRepository<DmlDeviceEvent, Long> {

    List<DmlDeviceEvent> findByDeviceSerialId(String deviceSerialId);

    List<DmlDeviceEvent> findByDeviceSerialIdAndEventDttmAfter(String deviceSerialId, LocalDateTime after);

    @Query("SELECT e FROM DmlDeviceEvent e WHERE e.deviceSerialId = :deviceSerialId ORDER BY e.eventDttm DESC")
    List<DmlDeviceEvent> findByDeviceSerialIdOrderByDttmDesc(@Param("deviceSerialId") String deviceSerialId);
}
