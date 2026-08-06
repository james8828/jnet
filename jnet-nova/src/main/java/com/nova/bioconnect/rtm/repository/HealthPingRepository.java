package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.HealthPingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthPingRepository extends JpaRepository<HealthPingEntity, Long> {
    List<HealthPingEntity> findByProcessNameAndHost(String processName, String host);
}