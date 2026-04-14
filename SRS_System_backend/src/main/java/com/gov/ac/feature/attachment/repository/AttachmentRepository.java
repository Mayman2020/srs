package com.gov.ac.feature.attachment.repository;

import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

  long countByCorrespondence_IdAndDeletedAtIsNull(UUID correspondenceId);

  @EntityGraph(attributePaths = {"correspondence", "correspondence.ownerDepartment"})
  @Query("select a from AttachmentEntity a where a.id = :id and a.deletedAt is null")
  Optional<AttachmentEntity> findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(@Param("id") Long id);

  @Query(
      "select distinct a from AttachmentEntity a left join fetch a.contentType "
          + "where a.correspondence.id = :correspondenceId and a.deletedAt is null "
          + "order by a.id asc")
  List<AttachmentEntity> findAllForDetailByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);
}
