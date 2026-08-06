package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.ObservationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationResultRepository extends JpaRepository<ObservationResultEntity, Long> {
    List<ObservationResultEntity> findBySampleKeyNum(String sampleKeyNum);
    List<ObservationResultEntity> findByTestCode(String testCode);
}