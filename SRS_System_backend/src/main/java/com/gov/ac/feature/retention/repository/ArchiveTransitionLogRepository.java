package com.gov.ac.feature.retention.repository;

import com.gov.ac.feature.retention.entity.ArchiveTransitionLogEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveTransitionLogRepository extends JpaRepository<ArchiveTransitionLogEntity, UUID> {

  Page<ArchiveTransitionLogEntity> findAllByOrderByExecutedAtDesc(Pageable pageable);
}
