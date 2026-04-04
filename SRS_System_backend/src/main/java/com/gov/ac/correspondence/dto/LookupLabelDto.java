package com.gov.ac.correspondence.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LookupLabelDto {
  String code;
  String nameAr;
  String nameEn;
}
