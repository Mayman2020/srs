package com.gov.ac.feature.lookups.controller;

import com.gov.ac.feature.lookups.dto.LookupBundleDto;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.lookups.service.LookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend: shared lookups (forms, filters). Angular path concept: {@code core/api/lookup.service}. */
@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
public class LookupController {

  private final LookupService lookupService;

  @GetMapping
  public LookupBundleDto bundle() {
    return lookupService.bundle();
  }

  @GetMapping("/priority")
  public List<LookupItemDto> priorityLookup() {
    return lookupService.priorityLookup();
  }
}
