package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.InstLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstLocationRepository extends JpaRepository<InstLocationEntity, Long> {
    Optional<InstLocationEntity> findByLocNum(String locNum);
    Optional<InstLocationEntity> findByParent(String parent);
    Optional<InstLocationEntity> findByLocName(String locName);
}