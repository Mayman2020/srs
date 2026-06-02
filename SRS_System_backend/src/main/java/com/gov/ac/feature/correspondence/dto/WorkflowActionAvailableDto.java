package com.gov.ac.feature.correspondence.dto;

/** A workflow action the current user may execute on a correspondence (from {@code workflow_action_type}). */
public record WorkflowActionAvailableDto(
    long id,
    String code,
    String nameAr,
    String nameEn,
    boolean requiresComment,
    boolean requiresSignature,
    int sortOrder,
    String uiVariant) {

  /** Legacy 7-arg overload kept for pre-Slice-5 callers; no signature requirement. */
  public WorkflowActionAvailableDto(
      long id,
      String code,
      String nameAr,
      String nameEn,
      boolean requiresComment,
      int sortOrder,
      String uiVariant) {
    this(id, code, nameAr, nameEn, requiresComment, false, sortOrder, uiVariant);
  }
}
