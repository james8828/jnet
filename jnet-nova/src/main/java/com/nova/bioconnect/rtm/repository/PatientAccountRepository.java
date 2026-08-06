package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.PatientAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientAccountRepository extends JpaRepository<PatientAccountEntity, Long> {
    Optional<PatientAccountEntity> findByAccountNum(String accountNum);
    List<PatientAccountEntity> findByPatientNum(String patientNum);
    Optional<PatientAccountEntity> findByAccountNumber(String accountNumber);
}