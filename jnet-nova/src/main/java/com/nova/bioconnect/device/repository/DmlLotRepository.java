package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Lot Repository
 * Mirrors C# DBA.lots queries
 */
@Repository
public interface DmlLotRepository extends JpaRepository<DmlLot, Long> {

    List<DmlLot> findByLot(String lot);

    @Query("SELECT l FROM DmlLot l WHERE l.lotType = :lotType AND (l.retired = 'F' OR l.retired IS NULL)")
    List<DmlLot> findActiveByLotType(@Param("lotType") String lotType);

    /**
     * Find lots linked to a device type and location (mirrors C# query)
     * Joins: dml_device_to_lot, dml_lot_to_unit
     */
    @Query("SELECT l FROM DmlLot l WHERE l.lotsKeyNum IN " +
           "(SELECT d.lotsKeyNum FROM DmlDeviceToLot d WHERE d.instType = :instType) " +
           "AND (l.retired = 'F' OR l.retired IS NULL) ORDER BY l.lot")
    List<DmlLot> findLotsForDevice(@Param("instType") String instType);

    @Query("SELECT COUNT(l) FROM DmlLot l WHERE l.lot = :lotNumber")
    long countByLot(@Param("lotNumber") String lotNumber);
}
