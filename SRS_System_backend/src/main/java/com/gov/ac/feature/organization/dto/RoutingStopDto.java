package com.gov.ac.feature.organization.dto;

/**
 * One stop in a Q/L/K/S routing chain. Stops include the department, its level code, and the
 * approver role that must act before the chain advances.
 *
 * @param departmentId database id of the department this stop targets
 * @param departmentCode stable {@code department.code} (audit-friendly)
 * @param departmentNameAr Arabic display name
 * @param departmentNameEn English display name
 * @param levelCode {@code Q} / {@code L} / {@code K} / {@code S}
 * @param roleCode canonical role expected to act at this stop (e.g. {@code DEPT_MANAGER})
 * @param reasonKey i18n message key explaining why this stop was added (e.g.
 *     {@code routing.viaParent})
 */
public record RoutingStopDto(
    Long departmentId,
    String departmentCode,
    String departmentNameAr,
    String departmentNameEn,
    String levelCode,
    String roleCode,
    String reasonKey) {}
