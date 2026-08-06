package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.QcResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcResultRepository extends JpaRepository<QcResultEntity, Long> {
    List<QcResultEntity> findBySampleKeyNum(String sampleKeyNum);
    List<QcResultEntity> findByLotNumber(String lotNumber);
}