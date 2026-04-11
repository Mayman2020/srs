package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.AttachmentIndexEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentIndexEntryRepository extends JpaRepository<AttachmentIndexEntry, Long> {

  @Query(
      "select e from AttachmentIndexEntry e where e.attachment.id = :aid and e.deletedAt is null "
          + "order by e.sortOrder asc, e.id asc")
  List<AttachmentIndexEntry> listActiveForAttachment(@Param("aid") Long attachmentId);

  @Query(
      "select e from AttachmentIndexEntry e where e.id = :id and e.attachment.id = :aid "
          + "and e.deletedAt is null")
  Optional<AttachmentIndexEntry> findActiveByIdAndAttachment(
      @Param("id") Long entryId, @Param("aid") Long attachmentId);
}
