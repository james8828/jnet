package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlLocLastUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DML Location Last Update Repository
 * Mirrors C# DBA.loc_last_update queries
 */
@Repository
public interface DmlLocLastUpdateRepository extends JpaRepository<DmlLocLastUpdate, Long> {

    List<DmlLocLastUpdate> findByLocNum(String locNum);

    Optional<DmlLocLastUpdate> findByLocNumAndDataType(String locNum, String dataType);

    /**
     * Count updates since a specific time
     * Mirrors C# query: SELECT count(*) from DBA.loc_last_update where loc_num=? and data_type=? and last_update_time >= ?
     */
    @Query("SELECT COUNT(l) FROM DmlLocLastUpdate l WHERE l.locNum = :locNum AND l.dataType = :dataType AND l.lastUpdateTime >= :since")
    long countUpdatesSince(@Param("locNum") String locNum,
                           @Param("dataType") String dataType,
                           @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM DmlLocLastUpdate l WHERE l.locNum = :locNum AND l.dataType = :dataType " +
           "AND l.instClass = :instClass AND l.lastUpdateTime >= :since")
    long countUpdatesSinceWithClass(@Param("locNum") String locNum,
                                    @Param("dataType") String dataType,
                                    @Param("instClass") String instClass,
                                    @Param("since") LocalDateTime since);
}
