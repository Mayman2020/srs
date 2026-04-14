package com.gov.ac.feature.attachment.mapper;

import com.gov.ac.feature.attachment.dto.AttachmentUploadResponseDto;

public final class AttachmentMapper {

  private AttachmentMapper() {}

  public static AttachmentUploadResponseDto toUploadResponse(
      String storageKey, long byteSize, String mimeType) {
    return new AttachmentUploadResponseDto(storageKey, byteSize, mimeType);
  }
}
