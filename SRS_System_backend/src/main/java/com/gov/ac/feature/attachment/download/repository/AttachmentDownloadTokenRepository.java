package com.gov.ac.feature.attachment.download.repository;

import com.gov.ac.feature.attachment.download.entity.AttachmentDownloadTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentDownloadTokenRepository
    extends JpaRepository<AttachmentDownloadTokenEntity, UUID> {

  @EntityGraph(attributePaths = {"attachment", "attachmentVersion", "attachment.correspondence"})
  Optional<AttachmentDownloadTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query("delete from AttachmentDownloadTokenEntity t where t.expiresAt < :cutoff")
  int deleteExpiredOlderThan(@Param("cutoff") Instant cutoff);
}
