package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientKindEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceRecipientKindRepository
    extends JpaRepository<CorrespondenceRecipientKindEntity, Long> {

  @Query(
      """
      SELECT k FROM CorrespondenceRecipientKindEntity k
      WHERE UPPER(k.code) = UPPER(:code)
        AND k.deletedAt IS NULL
        AND k.active = TRUE
      """)
  Optional<CorrespondenceRecipientKindEntity> findActiveByCode(@Param("code") String code);
}
