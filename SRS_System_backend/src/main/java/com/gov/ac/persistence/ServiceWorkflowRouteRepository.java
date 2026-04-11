package com.gov.ac.persistence;

import com.gov.ac.domain.workflow.ServiceWorkflowRoute;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceWorkflowRouteRepository extends JpaRepository<ServiceWorkflowRoute, Long> {

  @Query(
      "select r from ServiceWorkflowRoute r join fetch r.correspondenceType "
          + "where r.id = :id and r.deletedAt is null")
  Optional<ServiceWorkflowRoute> findByIdAndDeletedAtIsNull(@Param("id") Long id);

  Optional<ServiceWorkflowRoute> findFirstByCorrespondenceTypeIdAndDefaultRouteIsTrueAndActiveIsTrueAndDeletedAtIsNull(
      Long correspondenceTypeId);

  @Query(
      "select r from ServiceWorkflowRoute r join fetch r.correspondenceType "
          + "where r.correspondenceType.id = :tid and r.active = true and r.deletedAt is null "
          + "order by r.sortOrder asc")
  List<ServiceWorkflowRoute> findActiveRoutesForType(@Param("tid") Long tid);

  List<ServiceWorkflowRoute> findByCorrespondenceTypeIdAndDeletedAtIsNullOrderBySortOrderAsc(
      Long correspondenceTypeId);

  @Query(
      "select r from ServiceWorkflowRoute r join fetch r.correspondenceType "
          + "where r.deletedAt is null order by r.correspondenceType.id asc, r.sortOrder asc")
  List<ServiceWorkflowRoute> findByDeletedAtIsNullOrderByCorrespondenceTypeIdAscSortOrderAsc();
}
