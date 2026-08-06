package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlWifiSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DML WiFi Setup Repository
 * Mirrors C# DBA.wifi_setup queries
 */
@Repository
public interface DmlWifiSetupRepository extends JpaRepository<DmlWifiSetup, Long> {

    Optional<DmlWifiSetup> findByConfigId(String configId);

    /**
     * Find WiFi setup by location and instrument class
     * Mirrors C# query: SELECT config_id FROM DBA.loc_to_wifi_setup WHERE inst_class=? AND loc_num=?
     */
    @Query("SELECT w FROM DmlWifiSetup w WHERE w.configId IN " +
           "(SELECT l.configId FROM DmlLocToWifiSetup l WHERE l.instClass = :instClass AND l.locNum = :locNum)")
    List<DmlWifiSetup> findByLocNumAndInstClass(@Param("locNum") String locNum, @Param("instClass") String instClass);
}
