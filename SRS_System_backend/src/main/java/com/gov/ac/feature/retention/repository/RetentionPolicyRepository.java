package com.gov.ac.feature.retention.repository;

import com.gov.ac.feature.retention.entity.RetentionPolicyEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicyEntity, UUID> {

  List<RetentionPolicyEntity> findByEnabledTrueAndDeletedAtIsNullOrderByCodeAsc();

  List<RetentionPolicyEntity> findByDeletedAtIsNullOrderByCodeAsc();

  Optional<RetentionPolicyEntity> findByCodeAndDeletedAtIsNull(String code);
}
