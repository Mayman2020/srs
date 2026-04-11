package com.gov.ac.feature.organizations;

import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** External / government org tree for «الهيكل» screens (parallel to {@code /api/v1/departments}). */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

  private final OrganizationService organizationService;

  @GetMapping
  public List<OrganizationFlatDto> list() {
    return organizationService.listFlat();
  }
}
