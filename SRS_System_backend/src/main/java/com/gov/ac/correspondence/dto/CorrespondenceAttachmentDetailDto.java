package com.gov.ac.correspondence.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceAttachmentDetailDto {
  Long id;
  String displayName;
  boolean active;
  Long currentVersionId;
  LookupLabelDto contentType;
  List<AttachmentVersionDto> versions;
}
