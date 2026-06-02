package com.gov.ac.feature.attachment.dto;

/**
 * Server response from {@code POST /api/v1/attachments/upload}.
 *
 * <p>Slice 5 adds at-rest encryption metadata: when {@link #encryptionAlgo()} is non-null the
 * upload was AES-256-GCM encrypted and the registration call must echo these fields back so the
 * persistence layer can round-trip the version. {@code byteSize} is the <strong>plaintext</strong>
 * length so existing aggregate-size limits stay meaningful.
 */
public record AttachmentUploadResponseDto(
    String storageKey,
    long byteSize,
    String mimeType,
    String plaintextSha256,
    String encryptionAlgo,
    String encryptionKeyRef,
    String encryptionWrappedDekB64,
    String encryptionIvB64,
    String ciphertextSha256) {

  /** Legacy (pre-Slice-5) tuple — used by tests / consumers that don't care about crypto. */
  public AttachmentUploadResponseDto(String storageKey, long byteSize, String mimeType) {
    this(storageKey, byteSize, mimeType, null, null, null, null, null, null);
  }
}

