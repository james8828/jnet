package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.PatientVisitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientVisitRepository extends JpaRepository<PatientVisitEntity, Long> {
    Optional<PatientVisitEntity> findByVisitNum(String visitNum);
    List<PatientVisitEntity> findByPatientNum(String patientNum);
    List<PatientVisitEntity> findByAccountNum(String accountNum);
    Optional<PatientVisitEntity> findByVisitNumber(String visitNumber);
}