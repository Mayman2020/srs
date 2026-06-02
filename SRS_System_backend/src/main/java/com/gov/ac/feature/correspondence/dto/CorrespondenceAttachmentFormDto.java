package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorrespondenceAttachmentFormDto {

  @NotBlank
  @Size(max = 500)
  private String displayName;

  @NotBlank
  @Size(max = 2048)
  private String storageKey;

  @NotNull @Min(0)
  private Long byteSize;

  @Size(max = 200)
  private String mimeType;

  /** Optional {@code attachment_content_type.code} (e.g. PDF). */
  @Size(max = 64)
  private String contentTypeCode;

  @Size(max = 64)
  private String checksumSha256;

  // Slice 5 — at-rest encryption metadata returned by the upload endpoint.

  @Size(max = 64)
  private String plaintextSha256;

  @Size(max = 32)
  private String encryptionAlgo;

  @Size(max = 128)
  private String encryptionKeyRef;

  @Size(max = 8192)
  private String encryptionWrappedDekB64;

  @Size(max = 64)
  private String encryptionIvB64;

  @Size(max = 64)
  private String ciphertextSha256;
}
