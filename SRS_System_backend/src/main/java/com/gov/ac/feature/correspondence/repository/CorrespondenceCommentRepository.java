package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceCommentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceCommentRepository extends JpaRepository<CorrespondenceCommentEntity, Long> {

  @EntityGraph(attributePaths = {"author", "parentComment"})
  @Query(
      "select cc from CorrespondenceCommentEntity cc where cc.correspondence.id = :correspondenceId "
          + "and cc.deletedAt is null order by cc.createdAt asc, cc.id asc")
  List<CorrespondenceCommentEntity> findAllForDetailByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);

  @EntityGraph(attributePaths = {"author", "parentComment"})
  @Query(
      "select cc from CorrespondenceCommentEntity cc where cc.id = :id and cc.correspondence.id = "
          + ":correspondenceId and cc.deletedAt is null")
  java.util.Optional<CorrespondenceCommentEntity> findByIdAndCorrespondence_IdAndDeletedAtIsNull(
      @Param("id") Long id, @Param("correspondenceId") UUID correspondenceId);
}
