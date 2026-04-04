package com.gov.ac.attachment.dto;

public record AttachmentUploadResponse(String storageKey, long byteSize, String mimeType) {}
