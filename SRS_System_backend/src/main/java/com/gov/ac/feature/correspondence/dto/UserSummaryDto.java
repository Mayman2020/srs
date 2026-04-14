package com.gov.ac.feature.correspondence.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSummaryDto {
  UUID id;
  String username;
  String fullNameAr;
  String fullNameEn;
}
