package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorrespondenceAttachmentForm {

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
}
