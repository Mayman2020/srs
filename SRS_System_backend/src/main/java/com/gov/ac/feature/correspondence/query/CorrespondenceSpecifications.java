package com.gov.ac.feature.correspondence.query;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CorrespondenceSpecifications {

  private CorrespondenceSpecifications() {}

  public static Specification<CorrespondenceEntity> notDeleted() {
    return (root, q, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Non-privileged users: only items with an owner department equal to the viewer's department. */
  public static Specification<CorrespondenceEntity> visibleToOwnerDepartment(Long viewerDepartmentId) {
    return (root, q, cb) -> {
      Join<CorrespondenceEntity, DepartmentEntity> od = root.join("ownerDepartment", JoinType.INNER);
      return cb.equal(od.get("id"), viewerDepartmentId);
    };
  }

  public static Specification<CorrespondenceEntity> hasCorrespondenceStatusCode(String code) {
    String c = code.trim();
    return (root, q, cb) ->
        cb.equal(root.join("correspondenceStatus", JoinType.INNER).get("code"), c);
  }

  public static Specification<CorrespondenceEntity> hasCorrespondenceTypeCode(String code) {
    String c = code.trim();
    return (root, q, cb) ->
        cb.equal(root.join("correspondenceType", JoinType.INNER).get("code"), c);
  }

  public static Specification<CorrespondenceEntity> hasPriorityCode(String code) {
    String c = code.trim();
    return (root, q, cb) -> cb.equal(root.join("priority", JoinType.INNER).get("code"), c);
  }

  public static Specification<CorrespondenceEntity> createdAtOnOrAfter(Instant from) {
    return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<CorrespondenceEntity> createdAtOnOrBefore(Instant to) {
    return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

  /**
   * Case-insensitive substring search across {@code referenceNumber}, {@code subject} and
   * {@code externalReferenceNumber}. Cheap enough at the row counts we expect; replace with
   * a tsvector index if the corpus grows.
   */
  public static Specification<CorrespondenceEntity> matchesFreeText(String term) {
    String like = "%" + term.trim().toLowerCase() + "%";
    return (root, q, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("referenceNumber")), like),
            cb.like(cb.lower(root.get("subject")), like),
            cb.like(cb.lower(cb.coalesce(root.get("externalReferenceNumber"), "")), like),
            cb.like(cb.lower(cb.coalesce(root.get("barcodeValue"), "")), like));
  }

  /**
   * Hide items whose confidentiality {@code requires_clearance = TRUE} and whose
   * {@code sort_order} is more restrictive than the viewer's clearance (lower sort_order = more
   * restrictive — matches V1: TOP_SECRET=10, NORMAL=50). When the viewer has no clearance
   * assigned, restricted material is filtered out entirely.
   */
  public static Specification<CorrespondenceEntity> visibleByClearance(Integer viewerSortOrder) {
    return (root, q, cb) -> {
      Join<CorrespondenceEntity, ConfidentialityEntity> conf =
          root.join("confidentiality", JoinType.LEFT);
      Predicate notRestricted =
          cb.or(
              cb.isNull(conf.get("id")),
              cb.notEqual(conf.get("requiresClearance"), Boolean.TRUE));
      if (viewerSortOrder == null) {
        return notRestricted;
      }
      Predicate clearedForRestricted =
          cb.and(
              cb.equal(conf.get("requiresClearance"), Boolean.TRUE),
              cb.lessThanOrEqualTo(
                  cb.coalesce(conf.<Integer>get("sortOrder"), Integer.MAX_VALUE),
                  viewerSortOrder));
      return cb.or(notRestricted, clearedForRestricted);
    };
  }

  public static Specification<CorrespondenceEntity> forList(
      boolean viewerIsPrivileged,
      Long viewerDepartmentId,
      String statusCode,
      String typeCode,
      String priorityCode,
      Instant createdFrom,
      Instant createdTo,
      String freeText,
      Integer viewerClearanceSortOrder) {
    Specification<CorrespondenceEntity> spec = notDeleted();
    if (!viewerIsPrivileged) {
      spec = spec.and(visibleToOwnerDepartment(viewerDepartmentId));
    }
    spec = spec.and(visibleByClearance(viewerClearanceSortOrder));
    if (StringUtils.hasText(statusCode)) {
      spec = spec.and(hasCorrespondenceStatusCode(statusCode));
    }
    if (StringUtils.hasText(typeCode)) {
      spec = spec.and(hasCorrespondenceTypeCode(typeCode));
    }
    if (StringUtils.hasText(priorityCode)) {
      spec = spec.and(hasPriorityCode(priorityCode));
    }
    if (createdFrom != null) {
      spec = spec.and(createdAtOnOrAfter(createdFrom));
    }
    if (createdTo != null) {
      spec = spec.and(createdAtOnOrBefore(createdTo));
    }
    if (StringUtils.hasText(freeText)) {
      spec = spec.and(matchesFreeText(freeText));
    }
    return spec;
  }
}
