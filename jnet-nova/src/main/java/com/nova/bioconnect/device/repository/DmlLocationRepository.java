package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DML Location Repository
 * Mirrors C# DBA.inst_locations queries
 */
@Repository
public interface DmlLocationRepository extends JpaRepository<DmlLocation, Long> {

    Optional<DmlLocation> findByLocNum(String locNum);

    List<DmlLocation> findByLevelNum(Integer levelNum);

    List<DmlLocation> findByParentLocNum(String parentLocNum);

    /**
     * Find facility (level 1) locations
     * Mirrors C# query: SELECT loc_num FROM DBA.inst_locations WHERE level_num = 1
     */
    @Query("SELECT l FROM DmlLocation l WHERE l.levelNum = 1 ORDER BY l.facility")
    List<DmlLocation> findAllFacilities();

    /**
     * Find locations by facility name
     */
    @Query("SELECT l FROM DmlLocation l WHERE l.facility = :facility AND l.levelNum = 2 ORDER BY l.locName")
    List<DmlLocation> findLocationsByFacility(@Param("facility") String facility);

    /**
     * Find location by name and parent facility name
     * Mirrors C# query: select loc_num from DBA.inst_locations where loc_name=? and parent=(select loc_num from DBA.inst_locations where loc_name=? and level_num=1)
     */
    @Query("SELECT l FROM DmlLocation l WHERE l.locName = :locName AND l.parentLocNum = " +
           "(SELECT f.locNum FROM DmlLocation f WHERE f.locName = :facilityName AND f.levelNum = 1)")
    Optional<DmlLocation> findByLocationNameAndFacility(@Param("locName") String locName,
                                                         @Param("facilityName") String facilityName);

    /**
     * Find default locations
     */
    @Query("SELECT l FROM DmlLocation l WHERE l.isDefault = 'T' AND l.levelNum = 2")
    List<DmlLocation> findDefaultLocations();

    @Query("SELECT l FROM DmlLocation l WHERE l.isDefault = 'T' AND l.levelNum = 1")
    List<DmlLocation> findDefaultFacilities();
}
