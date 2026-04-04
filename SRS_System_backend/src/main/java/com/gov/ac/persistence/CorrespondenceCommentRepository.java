package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.CorrespondenceComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceCommentRepository extends JpaRepository<CorrespondenceComment, Long> {

  @EntityGraph(attributePaths = {"author", "parentComment"})
  @Query(
      "select cc from CorrespondenceComment cc where cc.correspondence.id = :correspondenceId "
          + "and cc.deletedAt is null order by cc.createdAt asc, cc.id asc")
  List<CorrespondenceComment> findAllForDetailByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);

  @EntityGraph(attributePaths = {"author", "parentComment"})
  @Query(
      "select cc from CorrespondenceComment cc where cc.id = :id and cc.correspondence.id = "
          + ":correspondenceId and cc.deletedAt is null")
  java.util.Optional<CorrespondenceComment> findByIdAndCorrespondence_IdAndDeletedAtIsNull(
      @Param("id") Long id, @Param("correspondenceId") UUID correspondenceId);
}
