package com.gov.ac.feature.workflow.routes.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;
import com.gov.ac.feature.workflow.routes.dto.ServiceWorkflowRouteDto;
import com.gov.ac.feature.workflow.routes.dto.UpsertServiceWorkflowRouteRequestDto;
import com.gov.ac.feature.workflow.routes.mapper.ServiceWorkflowRouteMapper;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.workflow.routes.repository.ServiceWorkflowRouteRepository;
import com.gov.ac.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ServiceWorkflowRouteService {

  private final ServiceWorkflowRouteRepository routeRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;

  @Transactional(readOnly = true)
  public List<ServiceWorkflowRouteDto> listActiveForTypeCode(String correspondenceTypeCode) {
    if (!StringUtils.hasText(correspondenceTypeCode)) {
      throw new BadRequestException("correspondenceTypeCode is required");
    }
    CorrespondenceTypeEntity type =
        correspondenceTypeRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(correspondenceTypeCode.trim())
            .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
    return routeRepository.findActiveRoutesForType(type.getId()).stream()
        .map(ServiceWorkflowRouteMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceWorkflowRouteDto> listAllForAdmin() {
    return routeRepository.findByDeletedAtIsNullOrderByCorrespondenceTypeIdAscSortOrderAsc().stream()
        .map(ServiceWorkflowRouteMapper::toDto)
        .toList();
  }

  @Transactional
  public ServiceWorkflowRouteDto create(UpsertServiceWorkflowRouteRequestDto req) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    CorrespondenceTypeEntity type = loadType(req.correspondenceTypeId());
    if (req.defaultRoute()) {
      clearOtherDefaults(type.getId(), null);
    }
    ServiceWorkflowRouteEntity r = new ServiceWorkflowRouteEntity();
    r.setCorrespondenceType(type);
    applyUpsert(r, req, actor);
    r = routeRepository.save(r);
    return ServiceWorkflowRouteMapper.toDto(r);
  }

  @Transactional
  public ServiceWorkflowRouteDto update(long id, UpsertServiceWorkflowRouteRequestDto req) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    ServiceWorkflowRouteEntity r =
        routeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Workflow route not found"));
    CorrespondenceTypeEntity type = loadType(req.correspondenceTypeId());
    if (req.defaultRoute()) {
      clearOtherDefaults(type.getId(), id);
    }
    r.setCorrespondenceType(type);
    applyUpsert(r, req, actor);
    r = routeRepository.save(r);
    return ServiceWorkflowRouteMapper.toDto(r);
  }

  @Transactional
  public void delete(long id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    ServiceWorkflowRouteEntity r =
        routeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Workflow route not found"));
    r.setDeletedAt(java.time.Instant.now());
    r.setDeletedBy(actor);
    r.setUpdatedBy(actor);
    routeRepository.save(r);
  }

  private void applyUpsert(ServiceWorkflowRouteEntity r, UpsertServiceWorkflowRouteRequestDto req, UUID actor) {
    r.setProcessDefinitionKey(req.processDefinitionKey().trim());
    r.setNameAr(req.nameAr().trim());
    r.setNameEn(req.nameEn().trim());
    r.setDefaultRoute(req.defaultRoute());
    r.setSortOrder(req.sortOrder());
    r.setActive(req.active());
    if (r.getCreatedBy() == null) {
      r.setCreatedBy(actor);
    }
    r.setUpdatedBy(actor);
  }

  private void clearOtherDefaults(long correspondenceTypeId, Long exceptRouteId) {
    List<ServiceWorkflowRouteEntity> rows =
        routeRepository.findByCorrespondenceTypeIdAndDeletedAtIsNullOrderBySortOrderAsc(
            correspondenceTypeId);
    for (ServiceWorkflowRouteEntity row : rows) {
      if (exceptRouteId != null && row.getId().equals(exceptRouteId)) {
        continue;
      }
      if (row.isDefaultRoute()) {
        row.setDefaultRoute(false);
        row.setUpdatedBy(SecurityUtils.requireCurrentUserId());
        routeRepository.save(row);
      }
    }
  }

  private CorrespondenceTypeEntity loadType(Long id) {
    return correspondenceTypeRepository
        .findById(id)
        .filter(t -> t.getDeletedAt() == null && Boolean.TRUE.equals(t.getActive()))
        .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
  }
}
