package com.gov.ac.feature.departments.service;

import com.gov.ac.feature.departments.dto.DepartmentFlatDto;
import com.gov.ac.feature.departments.mapper.DepartmentMapper;
import com.gov.ac.persistence.DepartmentRepository;
import java.util.List;
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
}
