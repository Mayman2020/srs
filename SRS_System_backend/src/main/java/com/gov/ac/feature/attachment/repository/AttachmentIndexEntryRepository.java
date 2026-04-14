package com.gov.ac.feature.attachment.repository;

import com.gov.ac.feature.attachment.entity.AttachmentIndexEntryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentIndexEntryRepository extends JpaRepository<AttachmentIndexEntryEntity, Long> {

  @Query(
      "select e from AttachmentIndexEntryEntity e where e.attachment.id = :aid and e.deletedAt is null "
          + "order by e.sortOrder asc, e.id asc")
  List<AttachmentIndexEntryEntity> listActiveForAttachment(@Param("aid") Long attachmentId);

  @Query(
      "select e from AttachmentIndexEntryEntity e where e.id = :id and e.attachment.id = :aid "
          + "and e.deletedAt is null")
  Optional<AttachmentIndexEntryEntity> findActiveByIdAndAttachment(
      @Param("id") Long entryId, @Param("aid") Long attachmentId);
}
