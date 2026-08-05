package com.nova.bioconnect.device.repository;

import com.nova.bioconnect.device.entity.DmlPhysician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DML Physician Repository
 */
@Repository
public interface DmlPhysicianRepository extends JpaRepository<DmlPhysician, Long> {

    Optional<DmlPhysician> findByPhysicianId(String physicianId);

    List<DmlPhysician> findByStatus(String status);

    @Query("SELECT p FROM DmlPhysician p WHERE p.status = 'Active' ORDER BY p.physicianId")
    List<DmlPhysician> findAllActive();

    @Query("SELECT p FROM DmlPhysician p WHERE p.locNum = :locNum AND p.status = 'Active'")
    List<DmlPhysician> findByLocNumAndStatus(@Param("locNum") String locNum, @Param("status") String status);

    boolean existsByPhysicianId(String physicianId);

    void deleteByPhysicianId(String physicianId);
}
