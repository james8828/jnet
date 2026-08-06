package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.MethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MethodRepository extends JpaRepository<MethodEntity, Long> {
    List<MethodEntity> findByOperatorNum(String operatorNum);
    List<MethodEntity> findByOperatorNumAndInstType(String operatorNum, String instType);
}