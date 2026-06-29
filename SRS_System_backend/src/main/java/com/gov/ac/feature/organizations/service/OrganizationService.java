package com.gov.ac.feature.organizations.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import com.gov.ac.feature.organizations.dto.UpsertOrganizationRequestDto;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.organizations.mapper.OrganizationMapper;
import com.gov.ac.feature.organizations.repository.OrganizationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrganizationService {

  private final OrganizationRepository organizationRepository;

  @Transactional(readOnly = true)
  public List<OrganizationFlatDto> listFlat() {
    return organizationRepository.findByDeletedAtIsNullOrderByIdAsc().stream()
        .map(OrganizationMapper::toFlat)
        .toList();
  }

  @Transactional
  public OrganizationFlatDto create(UUID actorId, UpsertOrganizationRequestDto request) {
    String code = normalizeCode(request.code());
    if (organizationRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Organization code already exists");
    }
    OrganizationEntity entity = new OrganizationEntity();
    entity.setCode(code);
    entity.setNameAr(normalizeText(request.nameAr()));
    entity.setNameEn(normalizeText(request.nameEn()));
    entity.setExternal(request.external());
    entity.setDescription(trimToNull(request.description()));
    entity.setParent(resolveParent(request.parentId(), null));
    entity.setCreatedBy(actorId);
    entity.setUpdatedBy(actorId);
    return OrganizationMapper.toFlat(organizationRepository.save(entity));
  }

  @Transactional
  public OrganizationFlatDto update(UUID actorId, long id, UpsertOrganizationRequestDto request) {
    OrganizationEntity entity = loadOrganization(id);
    String code = normalizeCode(request.code());
    if (organizationRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, entity.getId())) {
      throw new BadRequestException("Organization code already exists");
    }
    entity.setCode(code);
    entity.setNameAr(normalizeText(request.nameAr()));
    entity.setNameEn(normalizeText(request.nameEn()));
    entity.setExternal(request.external());
    entity.setDescription(trimToNull(request.description()));
    entity.setParent(resolveParent(request.parentId(), entity.getId()));
    entity.setUpdatedBy(actorId);
    return OrganizationMapper.toFlat(organizationRepository.save(entity));
  }

  @Transactional
  public void delete(UUID actorId, long id) {
    OrganizationEntity entity = loadOrganization(id);
    entity.setDeletedAt(Instant.now());
    entity.setDeletedBy(actorId);
    organizationRepository.save(entity);
  }

  private OrganizationEntity loadOrganization(long id) {
    return organizationRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("Organization not found"));
  }

  private OrganizationEntity resolveParent(Long parentId, Long selfId) {
    if (parentId == null) {
      return null;
    }
    if (selfId != null && parentId.equals(selfId)) {
      throw new BadRequestException("Organization cannot be its own parent");
    }
    return organizationRepository
        .findByIdAndDeletedAtIsNull(parentId)
        .orElseThrow(() -> new BadRequestException("Unknown parent organization"));
  }

  private static String normalizeCode(String code) {
    if (!StringUtils.hasText(code)) {
      throw new BadRequestException("code is required");
    }
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private static String normalizeText(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BadRequestException("name is required");
    }
    return value.trim();
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
