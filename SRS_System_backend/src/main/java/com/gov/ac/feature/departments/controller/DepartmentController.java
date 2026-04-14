package com.gov.ac.feature.departments.controller;

import com.gov.ac.feature.departments.dto.DepartmentFlatDto;
import com.gov.ac.feature.departments.dto.UpsertDepartmentRequestDto;
import com.gov.ac.feature.departments.service.DepartmentService;
import com.gov.ac.security.SecurityUtils;
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

/** Frontend: {@code features/departments} (or shared org tree). */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;

  @GetMapping
  public List<DepartmentFlatDto> list() {
    return departmentService.listActive();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has(authentication, 'lookup.manage')")
  public DepartmentFlatDto create(@Valid @RequestBody UpsertDepartmentRequestDto request) {
    return departmentService.create(SecurityUtils.requireCurrentUserId(), request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@effectivePermission.has(authentication, 'lookup.manage')")
  public DepartmentFlatDto update(
      @PathVariable long id, @Valid @RequestBody UpsertDepartmentRequestDto request) {
    return departmentService.update(SecurityUtils.requireCurrentUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has(authentication, 'lookup.manage')")
  public void delete(@PathVariable long id) {
    departmentService.delete(SecurityUtils.requireCurrentUserId(), id);
  }
}
