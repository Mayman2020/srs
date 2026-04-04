package com.gov.ac.correspondence.query;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.org.Department;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CorrespondenceSpecifications {

  private CorrespondenceSpecifications() {}

  public static Specification<Correspondence> notDeleted() {
    return (root, q, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Non-privileged users: only items with an owner department equal to the viewer's department. */
  public static Specification<Correspondence> visibleToOwnerDepartment(Long viewerDepartmentId) {
    return (root, q, cb) -> {
      Join<Correspondence, Department> od = root.join("ownerDepartment", JoinType.INNER);
      return cb.equal(od.get("id"), viewerDepartmentId);
    };
  }

  public static Specification<Correspondence> hasCorrespondenceStatusCode(String code) {
    String c = code.trim();
    return (root, q, cb) ->
        cb.equal(root.join("correspondenceStatus", JoinType.INNER).get("code"), c);
  }

  public static Specification<Correspondence> hasCorrespondenceTypeCode(String code) {
    String c = code.trim();
    return (root, q, cb) ->
        cb.equal(root.join("correspondenceType", JoinType.INNER).get("code"), c);
  }

  public static Specification<Correspondence> hasPriorityCode(String code) {
    String c = code.trim();
    return (root, q, cb) -> cb.equal(root.join("priority", JoinType.INNER).get("code"), c);
  }

  public static Specification<Correspondence> createdAtOnOrAfter(Instant from) {
    return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<Correspondence> createdAtOnOrBefore(Instant to) {
    return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

  public static Specification<Correspondence> forList(
      boolean viewerIsPrivileged,
      Long viewerDepartmentId,
      String statusCode,
      String typeCode,
      String priorityCode,
      Instant createdFrom,
      Instant createdTo) {
    Specification<Correspondence> spec = notDeleted();
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
