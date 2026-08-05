package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlObservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DML Observation Repository
 */
@Repository
public interface DmlObservationRepository extends JpaRepository<DmlObservation, Long> {

    /**
     * Find observations by device ID
     */
    List<DmlObservation> findByDeviceId(Long deviceId);

    /**
     * Find observations by sample key number
     */
    List<DmlObservation> findBySampleKeyNum(String sampleKeyNum);

    /**
     * Find observations by accession number
     */
    List<DmlObservation> findByAccessionNum(String accessionNum);

    /**
     * Find observations by patient ID
     */
    List<DmlObservation> findByPatientId(String patientId);

    /**
     * Find observations created after a specific time
     */
    List<DmlObservation> findByCreatedAtAfter(LocalDateTime after);

    /**
     * Find observations by device ID with pagination
     */
    Page<DmlObservation> findByDeviceId(Long deviceId, Pageable pageable);

    /**
     * Find all untransmitted observations
     */
    List<DmlObservation> findByTransmittedFlag(String transmittedFlag);

    /**
     * Count observations by device ID
     */
    long countByDeviceId(Long deviceId);
}