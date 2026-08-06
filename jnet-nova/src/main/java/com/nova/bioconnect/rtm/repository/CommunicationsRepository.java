package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.CommunicationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunicationsRepository extends JpaRepository<CommunicationsEntity, Long> {
    List<CommunicationsEntity> findByPortTypeAndComputerName(String portType, String computerName);
}