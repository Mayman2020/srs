package com.gov.ac.feature.attachment.mapper;

import com.gov.ac.feature.attachment.crypto.EncryptedBlobMetadata;
import com.gov.ac.feature.attachment.dto.AttachmentUploadResponseDto;
import java.util.Base64;

public final class AttachmentMapper {

  private AttachmentMapper() {}

  /** Legacy upload response (no encryption metadata). */
  public static AttachmentUploadResponseDto toUploadResponse(
      String storageKey, long byteSize, String mimeType) {
    return new AttachmentUploadResponseDto(storageKey, byteSize, mimeType);
  }

  /** Slice 5 upload response with at-rest encryption metadata. */
  public static AttachmentUploadResponseDto toEncryptedUploadResponse(
      String storageKey, String mimeType, EncryptedBlobMetadata metadata) {
    Base64.Encoder b64 = Base64.getEncoder();
    return new AttachmentUploadResponseDto(
        storageKey,
        metadata.plaintextByteSize(),
        mimeType,
        metadata.plaintextSha256(),
        metadata.algorithm(),
        metadata.keyRef(),
        b64.encodeToString(metadata.wrappedDek()),
        b64.encodeToString(metadata.iv()),
        metadata.ciphertextSha256());
  }
}

