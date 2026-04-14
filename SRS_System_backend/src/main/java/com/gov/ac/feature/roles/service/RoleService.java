package com.gov.ac.feature.roles.service;

import com.gov.ac.feature.roles.dto.RoleOptionDto;
import com.gov.ac.feature.roles.mapper.RoleMapper;
import com.gov.ac.feature.roles.repository.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

  private final RoleRepository roleRepository;

  @Transactional(readOnly = true)
  public List<RoleOptionDto> listActiveOptions() {
    return roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(RoleMapper::toOptionDto)
        .toList();
  }
}
