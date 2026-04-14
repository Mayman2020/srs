package com.gov.ac.feature.correspondence.query;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

  public static Specification<CorrespondenceEntity> forList(
      boolean viewerIsPrivileged,
      Long viewerDepartmentId,
      String statusCode,
      String typeCode,
      String priorityCode,
      Instant createdFrom,
      Instant createdTo) {
    Specification<CorrespondenceEntity> spec = notDeleted();
    if (!viewerIsPrivileged) {
      spec = spec.and(visibleToOwnerDepartment(viewerDepartmentId));
    }
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
    return spec;
  }
}
