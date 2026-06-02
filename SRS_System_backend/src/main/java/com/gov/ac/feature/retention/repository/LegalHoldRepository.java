package com.gov.ac.feature.retention.repository;

import com.gov.ac.feature.retention.entity.LegalHoldEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalHoldRepository extends JpaRepository<LegalHoldEntity, UUID> {

  boolean existsByCorrespondence_IdAndReleasedAtIsNullAndDeletedAtIsNull(UUID correspondenceId);

  boolean existsByCorrespondenceIsNullAndReleasedAtIsNullAndDeletedAtIsNull();

  List<LegalHoldEntity> findByReleasedAtIsNullAndDeletedAtIsNullOrderByPlacedAtDesc();
}
