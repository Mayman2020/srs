package com.gov.ac.feature.lookups.admin.dto;

/**
 * Full row for lookup admin UI. Optional fields are populated per physical table
 * (e.g. {@code terminal} only for correspondence_status).
 */
public record LookupRowAdminDto(
    Long id,
    String lookupCode,
    String code,
    String nameAr,
    String nameEn,
    String description,
    Integer sortOrder,
    Boolean active,
    Long parentId,
    Boolean terminal,
    Integer slaDays,
    Boolean restrictsExport,
    Boolean requiresClearance,
    /** {@code correspondence_status} only: badge key. */
    String uiVariant,
    /** {@code workflow_action_type} only. */
    Boolean requiresComment,
    /** {@code workflow_action_type} only. */
    Boolean requiresSignature) {

  /** Legacy 14-arg overload kept for callers that don't deal with {@code workflow_action_type}. */
  public LookupRowAdminDto(
      Long id,
      String lookupCode,
      String code,
      String nameAr,
      String nameEn,
      String description,
      Integer sortOrder,
      Boolean active,
      Long parentId,
      Boolean terminal,
      Integer slaDays,
      Boolean restrictsExport,
      Boolean requiresClearance,
      String uiVariant) {
    this(id, lookupCode, code, nameAr, nameEn, description, sortOrder, active, parentId,
        terminal, slaDays, restrictsExport, requiresClearance, uiVariant, null, null);
  }
}
