package com.gov.ac.feature.departments.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.dto.DepartmentFlatDto;
import com.gov.ac.feature.departments.dto.UpsertDepartmentRequestDto;
import com.gov.ac.feature.departments.mapper.DepartmentMapper;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

  private final DepartmentRepository departmentRepository;

  @Transactional(readOnly = true)
  public List<DepartmentFlatDto> listActive() {
    return departmentRepository.findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAsc().stream()
        .map(DepartmentMapper::toFlat)
        .toList();
  }

  @Transactional
  public DepartmentFlatDto create(UUID actorId, UpsertDepartmentRequestDto request) {
    String code = normalizeCode(request.code());
    if (departmentRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("departments.errors.codeExists");
    }

    DepartmentEntity entity = new DepartmentEntity();
    entity.setCode(code);
    entity.setNameAr(normalizeText(request.nameAr()));
    entity.setNameEn(normalizeText(request.nameEn()));
    entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
    entity.setActive(true);
    entity.setParent(resolveParent(request.parentId(), null));

    return DepartmentMapper.toFlat(departmentRepository.save(entity));
  }

  @Transactional
  public DepartmentFlatDto update(UUID actorId, long id, UpsertDepartmentRequestDto request) {
    DepartmentEntity entity = loadDepartment(id);
    String code = normalizeCode(request.code());
    if (departmentRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, entity.getId())) {
      throw new BadRequestException("departments.errors.codeExists");
    }

    entity.setCode(code);
    entity.setNameAr(normalizeText(request.nameAr()));
    entity.setNameEn(normalizeText(request.nameEn()));
    entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
    entity.setParent(resolveParent(request.parentId(), entity.getId()));

    return DepartmentMapper.toFlat(departmentRepository.save(entity));
  }

  @Transactional
  public void delete(UUID actorId, long id) {
    DepartmentEntity entity = loadDepartment(id);
    DepartmentEntity fallbackParent = entity.getParent();

    List<DepartmentEntity> children = departmentRepository.findByParent_IdAndDeletedAtIsNull(entity.getId());
    for (DepartmentEntity child : children) {
      child.setParent(fallbackParent);
    }
    departmentRepository.saveAll(children);

    entity.setActive(false);
    entity.setDeletedAt(Instant.now());
    entity.setDeletedBy(actorId);
    departmentRepository.save(entity);
  }

  private DepartmentEntity resolveParent(Long parentId, Long currentId) {
    if (parentId == null) {
      return null;
    }
    if (currentId != null && parentId.equals(currentId)) {
      throw new BadRequestException("departments.errors.parentSelf");
    }
    DepartmentEntity parent = loadDepartment(parentId);
    if (currentId != null) {
      ensureNoCycle(currentId, parent);
    }
    return parent;
  }

  private void ensureNoCycle(Long currentId, DepartmentEntity parent) {
    DepartmentEntity cursor = parent;
    while (cursor != null) {
      if (currentId.equals(cursor.getId())) {
        throw new BadRequestException("departments.errors.parentCycle");
      }
      DepartmentEntity next = cursor.getParent();
      if (next != null) {
        cursor = loadDepartment(next.getId());
      } else {
        cursor = null;
      }
    }
  }

  private DepartmentEntity loadDepartment(Long id) {
    return departmentRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("departments.errors.notFound"));
  }

  private String normalizeCode(String value) {
    return normalizeText(value).toUpperCase(Locale.ROOT);
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private int normalizeSortOrder(Integer value) {
    return value == null ? 0 : Math.max(value, 0);
  }
}
