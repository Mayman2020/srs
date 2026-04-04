package com.gov.ac.web;

import com.gov.ac.domain.user.Role;
import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.web.dto.LookupItemDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleRepository roleRepository;

  @GetMapping
  public List<LookupItemDto> list() {
    return roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }
}
