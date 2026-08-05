package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DML Patient Repository
 * Mirrors C# DBA.patients queries
 */
@Repository
public interface DmlPatientRepository extends JpaRepository<DmlPatient, Long> {

    Optional<DmlPatient> findByPatientId(String patientId);

    List<DmlPatient> findByStatus(String status);

    @Query("SELECT p FROM DmlPatient p WHERE p.status = 'Active' ORDER BY p.patientId")
    List<DmlPatient> findAllActive();

    @Query("SELECT p FROM DmlPatient p WHERE p.locNum = :locNum AND p.status = 'Active' ORDER BY p.patientId")
    List<DmlPatient> findByLocNumAndStatus(@Param("locNum") String locNum, @Param("status") String status);

    boolean existsByPatientId(String patientId);

    void deleteByPatientId(String patientId);

    Optional<DmlPatient> findByMedrecNum(String medrecNum);
}
