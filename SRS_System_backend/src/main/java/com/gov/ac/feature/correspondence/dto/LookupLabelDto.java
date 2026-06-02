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
  /**
   * Slice 5 — Optional flag mirrored from {@code confidentiality.requires_clearance}. Allows the
   * frontend to render a classified badge / gated download UI without fetching the full lookup.
   * Null for label types that don't carry clearance semantics.
   */
  Boolean requiresClearance;
}
