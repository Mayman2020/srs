package com.gov.ac.feature.attachment.signature.repository;

import com.gov.ac.feature.attachment.signature.entity.DocumentSignatureEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignatureEntity, UUID> {

  @EntityGraph(attributePaths = {"signer", "attachmentVersion", "attachmentVersion.attachment"})
  @Query("select s from DocumentSignatureEntity s where s.id = :id")
  Optional<DocumentSignatureEntity> findByIdLoaded(@Param("id") UUID id);

  @EntityGraph(attributePaths = {"signer", "attachmentVersion"})
  @Query(
      "select s from DocumentSignatureEntity s where s.attachmentVersion.id = :versionId order by s.signedAt asc")
  List<DocumentSignatureEntity> findByAttachmentVersionId(@Param("versionId") Long versionId);

  @EntityGraph(attributePaths = {"signer", "attachmentVersion"})
  @Query(
      "select s from DocumentSignatureEntity s where s.attachmentVersion.id = :versionId "
          + "and s.signer.id = :signerId and s.status = 'VALID'")
  Optional<DocumentSignatureEntity> findActiveByVersionAndSigner(
      @Param("versionId") Long versionId, @Param("signerId") UUID signerId);
}
