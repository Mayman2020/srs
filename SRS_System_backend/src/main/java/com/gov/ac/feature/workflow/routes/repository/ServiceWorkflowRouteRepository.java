package com.gov.ac.feature.workflow.routes.repository;

import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceWorkflowRouteRepository extends JpaRepository<ServiceWorkflowRouteEntity, Long> {

  @Query(
      "select r from ServiceWorkflowRouteEntity r join fetch r.correspondenceType "
          + "where r.id = :id and r.deletedAt is null")
  Optional<ServiceWorkflowRouteEntity> findByIdAndDeletedAtIsNull(@Param("id") Long id);

  Optional<ServiceWorkflowRouteEntity> findFirstByCorrespondenceTypeIdAndDefaultRouteIsTrueAndActiveIsTrueAndDeletedAtIsNull(
      Long correspondenceTypeId);

  @Query(
      "select r from ServiceWorkflowRouteEntity r join fetch r.correspondenceType "
          + "where r.correspondenceType.id = :tid and r.active = true and r.deletedAt is null "
          + "order by r.sortOrder asc")
  List<ServiceWorkflowRouteEntity> findActiveRoutesForType(@Param("tid") Long tid);

  List<ServiceWorkflowRouteEntity> findByCorrespondenceTypeIdAndDeletedAtIsNullOrderBySortOrderAsc(
      Long correspondenceTypeId);

  @Query(
      "select r from ServiceWorkflowRouteEntity r join fetch r.correspondenceType "
          + "where r.deletedAt is null order by r.correspondenceType.id asc, r.sortOrder asc")
  List<ServiceWorkflowRouteEntity> findByDeletedAtIsNullOrderByCorrespondenceTypeIdAscSortOrderAsc();
}
