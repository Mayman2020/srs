package com.gov.ac.feature.organizations.service;

import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import com.gov.ac.feature.organizations.mapper.OrganizationMapper;
import com.gov.ac.feature.organizations.repository.OrganizationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
