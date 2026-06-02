package com.gov.ac.feature.attachment.signature.dto;

import java.util.List;

/**
 * Public-ish verifier response: stable schema for QR / print readouts. Contains only what an
 * external auditor needs to confirm that a printed correspondence was signed by a specific user.
 */
public record AttachmentVerificationDto(
    Long attachmentVersionId,
    String plaintextSha256,
    String encryptionAlgo,
    List<DocumentSignatureDto> signatures) {}
