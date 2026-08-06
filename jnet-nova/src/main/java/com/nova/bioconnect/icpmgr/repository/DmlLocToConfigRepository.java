package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlLocToConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Location to Config mapping Repository
 */
@Repository
public interface DmlLocToConfigRepository extends JpaRepository<DmlLocToConfig, Long> {

    List<DmlLocToConfig> findByLocNum(String locNum);

    List<DmlLocToConfig> findByLocNumAndInstType(String locNum, String instType);
}
