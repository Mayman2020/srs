package com.gov.ac.feature.organizations;

import com.gov.ac.domain.org.Organization;
import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import com.gov.ac.persistence.OrganizationRepository;
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
        .map(OrganizationService::toFlat)
        .toList();
  }

  private static OrganizationFlatDto toFlat(Organization o) {
    return new OrganizationFlatDto(
        o.getId(),
        o.getParent() != null ? o.getParent().getId() : null,
        o.getCode(),
        o.getNameAr(),
        o.getNameEn(),
        Boolean.TRUE.equals(o.getExternal()));
  }
}
