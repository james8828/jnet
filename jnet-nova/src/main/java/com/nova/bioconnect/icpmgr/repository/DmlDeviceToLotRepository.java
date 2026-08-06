package com.nova.bioconnect.icpmgr.repository;

import com.nova.bioconnect.icpmgr.entity.DmlDeviceToLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DML Device to Lot mapping Repository
 * Mirrors C# DBA.device_to_lot queries
 */
@Repository
public interface DmlDeviceToLotRepository extends JpaRepository<DmlDeviceToLot, Long> {

    List<DmlDeviceToLot> findByInstType(String instType);

    List<DmlDeviceToLot> findByLotsKeyNum(String lotsKeyNum);
}
