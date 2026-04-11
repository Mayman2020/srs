package com.gov.ac.feature.lookups.dto;

/**
 * Shared lookup row for bundles and dropdowns. {@code parentId} is set when the row depends on
 * another lookup (e.g. correspondence_status → correspondence_type, classification tree parent).
 *
 * <p>Optional tail fields are table-specific; unused positions are null (JSON omits or null).
 */
public record LookupItemDto(
    Long id,
    String code,
    String nameAr,
    String nameEn,
    Integer sortOrder,
    Long parentId,
    Boolean terminal,
    String kpiSegment,
    Boolean dashboardInboundHighlight,
    Boolean dashboardOutboundHighlight,
    String uiVariant) {

  public LookupItemDto(
      Long id, String code, String nameAr, String nameEn, Integer sortOrder, Long parentId) {
    this(id, code, nameAr, nameEn, sortOrder, parentId, null, null, null, null, null);
  }

  public LookupItemDto(
      Long id,
      String code,
      String nameAr,
      String nameEn,
      Integer sortOrder,
      Long parentId,
      Boolean terminal,
      String kpiSegment) {
    this(id, code, nameAr, nameEn, sortOrder, parentId, terminal, kpiSegment, null, null, null);
  }

  public LookupItemDto(
      Long id,
      String code,
      String nameAr,
      String nameEn,
      Integer sortOrder,
      Long parentId,
      Boolean terminal,
      String kpiSegment,
      Boolean dashboardInboundHighlight,
      Boolean dashboardOutboundHighlight) {
    this(
        id,
        code,
        nameAr,
        nameEn,
        sortOrder,
        parentId,
        terminal,
        kpiSegment,
        dashboardInboundHighlight,
        dashboardOutboundHighlight,
        null);
  }
}
