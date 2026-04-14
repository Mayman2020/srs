package com.gov.ac.feature.correspondence.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class CorrespondenceListPageables {

  private static final int MAX_PAGE_SIZE = 100;

  /** API sort property name → JPA path (whitelist only). */
  private static final Map<String, String> SORT_FIELDS =
      Map.ofEntries(
          Map.entry("createdAt", "createdAt"),
          Map.entry("updatedAt", "updatedAt"),
          Map.entry("referenceNumber", "referenceNumber"),
          Map.entry("subject", "subject"),
          Map.entry("dueDate", "dueDate"),
          Map.entry("status", "correspondenceStatus.code"),
          Map.entry("type", "correspondenceType.code"),
          Map.entry("priority", "priority.code"));

  private CorrespondenceListPageables() {}

  public static Pageable sanitize(Pageable pageable) {
    int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
    Sort sort = sanitizeSort(pageable.getSort());
    return PageRequest.of(pageable.getPageNumber(), size, sort);
  }

  private static Sort sanitizeSort(Sort sort) {
    List<Sort.Order> orders = new ArrayList<>();
    for (Sort.Order o : sort) {
      String path = SORT_FIELDS.get(o.getProperty());
      if (path != null) {
        orders.add(new Sort.Order(o.getDirection(), path));
      }
    }
    if (orders.isEmpty()) {
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    return Sort.by(orders);
  }
}
