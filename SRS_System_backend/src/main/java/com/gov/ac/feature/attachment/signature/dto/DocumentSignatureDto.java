package com.gov.ac.feature.attachment.signature.dto;

import java.time.Instant;
import java.util.UUID;

/** Read-only projection of a {@code document_signature} row exposed to the UI / verifier. */
public record DocumentSignatureDto(
    UUID id,
    Long attachmentId,
    Long attachmentVersionId,
    UUID signerUserId,
    String signerUsername,
    String signerFullNameAr,
    String signerFullNameEn,
    String algorithm,
    String canonicalHashSha256,
    String keyRef,
    Instant signedAt,
    String status,
    String verificationStatus,
    Instant verificationAt,
    String verificationDetail) {}
