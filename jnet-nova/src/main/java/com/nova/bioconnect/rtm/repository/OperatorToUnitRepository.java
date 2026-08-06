package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.OperatorToUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorToUnitRepository extends JpaRepository<OperatorToUnitEntity, Long> {
    List<OperatorToUnitEntity> findByOperatorNum(String operatorNum);
    List<OperatorToUnitEntity> findByLocNum(String locNum);
}