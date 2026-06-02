package com.gov.ac.feature.attachment.verification.repository;

import com.gov.ac.feature.attachment.verification.entity.AttachmentVerificationTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentVerificationTokenRepository
    extends JpaRepository<AttachmentVerificationTokenEntity, UUID> {

  @EntityGraph(attributePaths = {"attachment", "attachmentVersion", "attachment.correspondence"})
  Optional<AttachmentVerificationTokenEntity> findByTokenHash(String tokenHash);

  @EntityGraph(attributePaths = {"attachment", "attachmentVersion"})
  List<AttachmentVerificationTokenEntity> findByAttachmentVersion_IdOrderByIssuedAtDesc(Long versionId);
}
