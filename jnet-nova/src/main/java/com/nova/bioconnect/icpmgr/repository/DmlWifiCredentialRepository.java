package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlWifiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML WiFi Credential Repository
 * Mirrors C# DBA.wifi_credentials queries
 */
@Repository
public interface DmlWifiCredentialRepository extends JpaRepository<DmlWifiCredential, Long> {

    /**
     * Find credentials by facility, location and MAC address
     * Mirrors C# "1FacAnd1Location" query
     */
    @Query("SELECT w FROM DmlWifiCredential w WHERE w.facNum = :facNum AND w.locNum = :locNum AND w.wifiMacAddress = :macAddress")
    List<DmlWifiCredential> findByFacilityAndLocationAndMac(
            @Param("facNum") String facNum,
            @Param("locNum") String locNum,
            @Param("macAddress") String macAddress);

    /**
     * Find credentials by facility, all locations and MAC address
     * Mirrors C# "1FacAndAllLocation" query
     */
    @Query("SELECT w FROM DmlWifiCredential w WHERE w.facNum = :facNum AND (w.locNum = 'All' OR w.locNum = '' OR w.locNum IS NULL) AND w.wifiMacAddress = :macAddress")
    List<DmlWifiCredential> findByFacilityAllLocationsAndMac(
            @Param("facNum") String facNum,
            @Param("macAddress") String macAddress);

    /**
     * Find credentials by all facilities and MAC address
     * Mirrors C# "AllFac" query
     */
    @Query("SELECT w FROM DmlWifiCredential w WHERE (w.facNum = 'All' OR w.facNum = '' OR w.facNum IS NULL) AND w.wifiMacAddress = :macAddress")
    List<DmlWifiCredential> findAllFacilitiesAndMac(@Param("macAddress") String macAddress);
}
