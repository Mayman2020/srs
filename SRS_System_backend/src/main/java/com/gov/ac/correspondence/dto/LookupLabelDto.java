package com.gov.ac.correspondence.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LookupLabelDto {
  String code;
  String nameAr;
  String nameEn;
  /** Correspondence status only: badge key from {@code correspondence_status.ui_variant}. */
  String uiVariant;
}
