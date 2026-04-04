package com.gov.ac.correspondence.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttachmentVersionDto {
  Long id;
  int versionNumber;
  long byteSize;
  String mimeType;
  String checksumSha256;
  Instant createdAt;
}
