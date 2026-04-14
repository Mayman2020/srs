package com.gov.ac.feature.correspondence.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LookupLabelDto {
  String code;
  String nameAr;
  String nameEn;
  /** CorrespondenceEntity status only: badge key from {@code correspondence_status.ui_variant}. */
  String uiVariant;
}
