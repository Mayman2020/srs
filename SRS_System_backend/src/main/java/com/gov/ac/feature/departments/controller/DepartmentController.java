package com.gov.ac.feature.departments.controller;

import com.gov.ac.feature.departments.dto.DepartmentFlatDto;
import com.gov.ac.feature.departments.service.DepartmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
