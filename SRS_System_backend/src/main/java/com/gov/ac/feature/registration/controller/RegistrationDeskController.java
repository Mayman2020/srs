package com.gov.ac.feature.registration.controller;

import com.gov.ac.feature.registration.dto.RegistrationDeskIntakeRequestDto;
import com.gov.ac.feature.registration.dto.RegistrationDeskIntakeResponseDto;
import com.gov.ac.feature.registration.dto.RegistrationDeskRowDto;
import com.gov.ac.feature.registration.service.RegistrationDeskService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registration-desk")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RegistrationDeskController {

  private final RegistrationDeskService registrationDeskService;

  @PostMapping("/intake")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE')")
  public RegistrationDeskIntakeResponseDto intake(@Valid @RequestBody RegistrationDeskIntakeRequestDto body) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    return registrationDeskService.intake(actor, body);
  }

  @GetMapping("/today")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
  public List<RegistrationDeskRowDto> today(@RequestParam String deskMode) {
    return registrationDeskService.todayRegister(deskMode);
  }
}
