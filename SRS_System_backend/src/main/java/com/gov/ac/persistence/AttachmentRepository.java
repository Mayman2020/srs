package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.Attachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  @EntityGraph(attributePaths = {"correspondence", "correspondence.ownerDepartment"})
  @Query("select a from Attachment a where a.id = :id and a.deletedAt is null")
  Optional<Attachment> findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(@Param("id") Long id);

  @Query(
      "select distinct a from Attachment a left join fetch a.contentType "
          + "where a.correspondence.id = :correspondenceId and a.deletedAt is null "
          + "order by a.id asc")
  List<Attachment> findAllForDetailByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);
}
