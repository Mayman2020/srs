package com.gov.ac.feature.lookups.admin.controller;

import com.gov.ac.feature.lookups.admin.dto.LookupCatalogDto;
import com.gov.ac.feature.lookups.admin.dto.LookupRowAdminDto;
import com.gov.ac.feature.lookups.admin.dto.LookupUpsertRequestDto;
import com.gov.ac.feature.lookups.admin.service.LookupTableAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/lookup-tables")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('ADMIN_LOOKUP_MANAGE')")
public class LookupTableAdminController {

  private final LookupTableAdminService lookupTableAdminService;

  @GetMapping("/catalog")
  public List<LookupCatalogDto> catalog() {
    return lookupTableAdminService.listCatalog();
  }

  @GetMapping("/{lookupCode}/rows")
  public List<LookupRowAdminDto> listRows(@PathVariable String lookupCode) {
    return lookupTableAdminService.listRows(lookupCode);
  }

  @PostMapping("/{lookupCode}/rows")
  @ResponseStatus(HttpStatus.CREATED)
  public LookupRowAdminDto create(
      @PathVariable String lookupCode, @Valid @RequestBody LookupUpsertRequestDto body) {
    return lookupTableAdminService.create(lookupCode, body);
  }

  @PutMapping("/{lookupCode}/rows/{id}")
  public LookupRowAdminDto update(
      @PathVariable String lookupCode,
      @PathVariable Long id,
      @Valid @RequestBody LookupUpsertRequestDto body) {
    return lookupTableAdminService.update(lookupCode, id, body);
  }

  @DeleteMapping("/{lookupCode}/rows/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String lookupCode, @PathVariable Long id) {
    lookupTableAdminService.delete(lookupCode, id);
  }
}
