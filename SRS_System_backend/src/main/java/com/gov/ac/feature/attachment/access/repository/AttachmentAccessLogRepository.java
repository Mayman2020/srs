package com.gov.ac.feature.attachment.access.repository;

import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentAccessLogRepository
    extends JpaRepository<AttachmentAccessLogEntity, Long> {

  @Query(
      "select l from AttachmentAccessLogEntity l "
          + "join fetch l.user u "
          + "where l.attachment.id = :attachmentId "
          + "order by l.occurredAt desc, l.id desc")
  List<AttachmentAccessLogEntity> findRecentByAttachmentId(
      @Param("attachmentId") Long attachmentId);

  @Query(
      "select l from AttachmentAccessLogEntity l "
          + "join fetch l.user u "
          + "where l.correspondence.id = :correspondenceId "
          + "order by l.occurredAt desc, l.id desc")
  List<AttachmentAccessLogEntity> findRecentByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);

  @Query(
      value =
          "select l from AttachmentAccessLogEntity l join fetch l.user u order by l.occurredAt desc, l.id desc",
      countQuery = "select count(l) from AttachmentAccessLogEntity l")
  Page<AttachmentAccessLogEntity> findAllPaged(Pageable pageable);
}
