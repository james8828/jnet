package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DML Operator Repository
 * Mirrors C# DBA.operators queries
 */
@Repository
public interface DmlOperatorRepository extends JpaRepository<DmlOperator, Long> {

    Optional<DmlOperator> findByOperatorId(String operatorId);

    List<DmlOperator> findByStatus(String status);

    @Query("SELECT o FROM DmlOperator o WHERE o.status = 'Active' ORDER BY o.operatorId")
    List<DmlOperator> findAllActive();

    @Query("SELECT o FROM DmlOperator o WHERE o.locNum = :locNum AND o.status = 'Active' ORDER BY o.operatorId")
    List<DmlOperator> findByLocNumAndStatus(@Param("locNum") String locNum, @Param("status") String status);

    boolean existsByOperatorId(String operatorId);

    void deleteByOperatorId(String operatorId);
}
