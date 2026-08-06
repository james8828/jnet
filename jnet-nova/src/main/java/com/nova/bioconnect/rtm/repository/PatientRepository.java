package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
    Optional<PatientEntity> findByPatientNum(String patientNum);
    Optional<PatientEntity> findByMedrecNum(String medrecNum);
    Optional<PatientEntity> findByPatientId(String patientId);
}