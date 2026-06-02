package com.gov.ac.feature.organization.service;

import com.gov.ac.feature.organization.dto.OrganizationalUnitLevelDto;
import com.gov.ac.feature.organization.entity.OrganizationalUnitLevelEntity;
import com.gov.ac.feature.organization.repository.OrganizationalUnitLevelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationalUnitLevelService {

  private final OrganizationalUnitLevelRepository repository;

  @Transactional(readOnly = true)
  public List<OrganizationalUnitLevelDto> listActive() {
    return repository.findAllActive().stream().map(this::toDto).toList();
  }

  private OrganizationalUnitLevelDto toDto(OrganizationalUnitLevelEntity entity) {
    return new OrganizationalUnitLevelDto(
        entity.getId(),
        entity.getCode(),
        entity.getNameAr(),
        entity.getNameEn(),
        entity.getDescription(),
        entity.getRankOrder(),
        entity.getActive());
  }
}
