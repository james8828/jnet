package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.OperatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<OperatorEntity, Long> {
    Optional<OperatorEntity> findByOperatorNum(String operatorNum);
    Optional<OperatorEntity> findByOperatorId(String operatorId);
}