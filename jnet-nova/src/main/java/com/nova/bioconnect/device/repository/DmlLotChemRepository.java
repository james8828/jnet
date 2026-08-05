package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlLotChem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Lot Chemistry Repository
 * Mirrors C# DBA.lot_chem queries
 */
@Repository
public interface DmlLotChemRepository extends JpaRepository<DmlLotChem, Long> {

    List<DmlLotChem> findByLotsKeyNum(String lotsKeyNum);

    /**
     * Find lot chemistry data joined with lot info
     * Mirrors C# query: SELECT lot_level, level_type, generic_test_name, LR, HR, Units
     */
    @Query("SELECT lc FROM DmlLotChem lc WHERE lc.lotsKeyNum = :lotsKeyNum")
    List<DmlLotChem> findChemistryByLotKey(@Param("lotsKeyNum") String lotsKeyNum);
}
