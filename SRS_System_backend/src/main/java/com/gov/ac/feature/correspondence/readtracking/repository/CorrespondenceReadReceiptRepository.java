package com.gov.ac.feature.correspondence.readtracking.repository;

import com.gov.ac.feature.correspondence.readtracking.entity.CorrespondenceReadReceiptEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceReadReceiptRepository
    extends JpaRepository<CorrespondenceReadReceiptEntity, Long> {

  @Query(
      "select r from CorrespondenceReadReceiptEntity r "
          + "where r.correspondence.id = :correspondenceId "
          + "and r.user.id = :userId "
          + "and r.deletedAt is null")
  Optional<CorrespondenceReadReceiptEntity> findActiveByCorrespondenceAndUser(
      @Param("correspondenceId") UUID correspondenceId, @Param("userId") UUID userId);

  @Query(
      "select r from CorrespondenceReadReceiptEntity r "
          + "join fetch r.user u "
          + "where r.correspondence.id = :correspondenceId "
          + "and r.deletedAt is null "
          + "order by r.firstOpenedAt asc, r.id asc")
  List<CorrespondenceReadReceiptEntity> findAllActiveByCorrespondence(
      @Param("correspondenceId") UUID correspondenceId);
}
