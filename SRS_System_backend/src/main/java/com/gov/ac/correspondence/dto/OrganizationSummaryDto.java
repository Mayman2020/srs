package com.gov.ac.correspondence.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationSummaryDto {
  Long id;
  String code;
  String nameAr;
  String nameEn;
}
