package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DML Order Repository
 * Mirrors C# DBA.orders queries
 */
@Repository
public interface DmlOrderRepository extends JpaRepository<DmlOrder, Long> {

    Optional<DmlOrder> findByAccessionNum(String accessionNum);

    void deleteByAccessionNum(String accessionNum);

    boolean existsByAccessionNum(String accessionNum);
}
