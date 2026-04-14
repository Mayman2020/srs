package com.gov.ac.feature.correspondence.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceCommentDetailDto {
  Long id;
  String body;
  Instant createdAt;
  Long parentCommentId;
  UserSummaryDto author;
}
