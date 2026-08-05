package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlSampleData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DML Sample Data Repository
 * Mirrors C# DBA.samples queries
 */
@Repository
public interface DmlSampleDataRepository extends JpaRepository<DmlSampleData, Long> {

    List<DmlSampleData> findByDeviceSerialId(String deviceSerialId);

    List<DmlSampleData> findByAccessionNum(String accessionNum);

    List<DmlSampleData> findByPatientId(String patientId);

    /**
     * Check if sample already exists (mirrors C# count query)
     * SELECT count(*) FROM DBA.samples WHERE sample_Date=? AND device_serial=?
     */
    @Query("SELECT COUNT(s) FROM DmlSampleData s WHERE s.sampleDate = :sampleDate AND s.deviceSerialId = :deviceSerialId")
    long countBySampleDateAndDevice(@Param("sampleDate") LocalDateTime sampleDate,
                                     @Param("deviceSerialId") String deviceSerialId);

    @Query("SELECT s FROM DmlSampleData s WHERE s.transmittedFlag = 'F' ORDER BY s.sampleDate")
    List<DmlSampleData> findUntransmitted();
}
