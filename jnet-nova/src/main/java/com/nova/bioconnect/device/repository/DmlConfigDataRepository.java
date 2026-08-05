package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlConfigData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Config Data Repository
 * Mirrors C# DBA.config_data queries
 */
@Repository
public interface DmlConfigDataRepository extends JpaRepository<DmlConfigData, Long> {

    @Query("SELECT c FROM DmlConfigData c WHERE c.configNum IN " +
           "(SELECT l.configNum FROM DmlLocToConfig l WHERE l.locNum = :locNum)")
    List<DmlConfigData> findByLocNum(@Param("locNum") String locNum);

    @Query("SELECT c FROM DmlConfigData c WHERE c.configNum IN " +
           "(SELECT l.configNum FROM DmlLocToConfig l WHERE l.locNum = :locNum AND l.instType = :instType)")
    List<DmlConfigData> findByLocNumAndInstType(@Param("locNum") String locNum, @Param("instType") String instType);
}
