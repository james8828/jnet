package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
    Optional<SampleEntity> findBySampleKeyNum(String sampleKeyNum);
    List<SampleEntity> findByTransmittedFlag(String transmittedFlag);
    List<SampleEntity> findByPatientNum(String patientNum);
    List<SampleEntity> findByDeviceSerial(String deviceSerial);
}