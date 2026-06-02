package com.gov.ac.feature.attachment.verification.dto;

import java.time.Instant;
import java.util.List;

/**
 * Scrubbed projection returned by the permitAll public verify endpoint. Deliberately omits
 * subject / body / confidentiality / email / IP — QR consumers see only what an external auditor
 * needs to confirm the printed correspondence was signed by a specific party at a specific time.
 */
public record AttachmentPublicVerificationDto(
    Long attachmentVersionId,
    String plaintextSha256,
    String encryptionAlgo,
    Instant issuedAt,
    String correspondenceReferenceNumber,
    String organizationLabel,
    List<PublicSignatureDto> signatures) {

  public record PublicSignatureDto(
      String signerDisplayName,
      String algorithm,
      Instant signedAt,
      String status,
      String verificationStatus) {}
}
