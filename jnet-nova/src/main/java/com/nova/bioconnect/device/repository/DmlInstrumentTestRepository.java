package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlInstrumentTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Instrument Test Repository
 * Mirrors C# DBA.instruments_tests queries
 */
@Repository
public interface DmlInstrumentTestRepository extends JpaRepository<DmlInstrumentTest, Long> {

    List<DmlInstrumentTest> findByInstType(String instType);

    @Query("SELECT t FROM DmlInstrumentTest t WHERE t.instType = :instType ORDER BY t.uiOrder")
    List<DmlInstrumentTest> findByInstTypeOrdered(@Param("instType") String instType);

    @Query("SELECT DISTINCT t FROM DmlInstrumentTest t WHERE t.instType = :instType " +
           "AND (t.enableAllAges = 'T' OR t.enableAllAges IS NULL) ORDER BY t.uiOrder")
    List<DmlInstrumentTest> findEnabledTests(@Param("instType") String instType);
}
