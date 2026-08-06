package com.nova.bioconnect.rtm.repository;

import com.nova.bioconnect.rtm.entity.LocLastUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocLastUpdateRepository extends JpaRepository<LocLastUpdateEntity, Long> {
    Optional<LocLastUpdateEntity> findByLocNumAndDataType(String locNum, String dataType);
}