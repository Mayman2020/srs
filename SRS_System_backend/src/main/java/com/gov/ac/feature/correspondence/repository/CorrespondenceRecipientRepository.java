package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceRecipientRepository extends JpaRepository<CorrespondenceRecipientEntity, Long> {

  @Query(
      """
      SELECT r FROM CorrespondenceRecipientEntity r
      JOIN FETCH r.department d
      WHERE r.correspondence.id = :correspondenceId
        AND r.deletedAt IS NULL
      ORDER BY d.nameAr ASC
      """)
  List<CorrespondenceRecipientEntity> listActiveForCorrespondence(
      @Param("correspondenceId") UUID correspondenceId);

  @Query(
      """
      SELECT r FROM CorrespondenceRecipientEntity r
      JOIN FETCH r.department d
      WHERE r.id = :id
        AND r.correspondence.id = :correspondenceId
        AND r.deletedAt IS NULL
      """)
  Optional<CorrespondenceRecipientEntity> findActiveByIdAndCorrespondence(
      @Param("id") long id, @Param("correspondenceId") UUID correspondenceId);

  @Query(
      """
      SELECT COUNT(r) > 0 FROM CorrespondenceRecipientEntity r
      WHERE r.correspondence.id = :correspondenceId
        AND r.department.id = :departmentId
        AND r.deletedAt IS NULL
      """)
  boolean existsActivePair(
      @Param("correspondenceId") UUID correspondenceId, @Param("departmentId") long departmentId);
}
