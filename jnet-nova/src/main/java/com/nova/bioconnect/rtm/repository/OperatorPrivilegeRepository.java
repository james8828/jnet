package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.OperatorPrivilegeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorPrivilegeRepository extends JpaRepository<OperatorPrivilegeEntity, Long> {
    List<OperatorPrivilegeEntity> findByOperatorNum(String operatorNum);
    List<OperatorPrivilegeEntity> findByOperatorNumAndInstType(String operatorNum, String instType);
}