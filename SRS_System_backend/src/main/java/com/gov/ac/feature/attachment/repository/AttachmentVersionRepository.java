package com.gov.ac.feature.attachment.repository;

import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentVersionRepository extends JpaRepository<AttachmentVersionEntity, Long> {

  @EntityGraph(attributePaths = "attachment")
  @Query("select v from AttachmentVersionEntity v where v.id = :id and v.deletedAt is null")
  Optional<AttachmentVersionEntity> findByIdAndDeletedAtIsNullWithAttachment(@Param("id") Long id);

  @Query(
      "select v from AttachmentVersionEntity v join fetch v.attachment att "
          + "where att.id in :attachmentIds and v.deletedAt is null "
          + "order by att.id asc, v.versionNumber asc")
  List<AttachmentVersionEntity> findAllForDetailByAttachmentIdIn(
      @Param("attachmentIds") Collection<Long> attachmentIds);
}
