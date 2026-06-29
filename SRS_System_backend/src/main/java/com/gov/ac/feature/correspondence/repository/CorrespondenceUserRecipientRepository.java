package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceUserRecipientEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceUserRecipientRepository
    extends JpaRepository<CorrespondenceUserRecipientEntity, Long> {

  @Query(
      """
      SELECT r FROM CorrespondenceUserRecipientEntity r
      JOIN FETCH r.recipientUser u
      JOIN FETCH r.recipientKind k
      WHERE r.correspondence.id = :correspondenceId
        AND r.deletedAt IS NULL
      ORDER BY k.sortOrder ASC, u.fullNameAr ASC
      """)
  List<CorrespondenceUserRecipientEntity> listActiveForCorrespondence(
      @Param("correspondenceId") UUID correspondenceId);

  @Query(
      """
      SELECT r FROM CorrespondenceUserRecipientEntity r
      WHERE r.id = :id
        AND r.correspondence.id = :correspondenceId
        AND r.deletedAt IS NULL
      """)
  Optional<CorrespondenceUserRecipientEntity> findActiveByIdAndCorrespondence(
      @Param("id") long id, @Param("correspondenceId") UUID correspondenceId);

  @Query(
      """
      SELECT COUNT(r) > 0 FROM CorrespondenceUserRecipientEntity r
      WHERE r.correspondence.id = :correspondenceId
        AND r.recipientUser.id = :recipientUserId
        AND r.recipientKind.id = :kindId
        AND r.deletedAt IS NULL
      """)
  boolean existsActiveTriple(
      @Param("correspondenceId") UUID correspondenceId,
      @Param("recipientUserId") UUID recipientUserId,
      @Param("kindId") long kindId);
}
