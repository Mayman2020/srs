package com.gov.ac.feature.leave.controller;

import com.gov.ac.feature.leave.service.LeaveRequestService;
import com.gov.ac.feature.leave.dto.DecideLeaveRequestDto;
import com.gov.ac.feature.leave.dto.LeaveRequestDto;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/leave-requests")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has(authentication, 'leave.admin')")
public class LeaveRequestAdminController {

  private final LeaveRequestService leaveRequestService;

  @GetMapping
  public List<LeaveRequestDto> listAll() {
    return leaveRequestService.listAll();
  }

  @PatchMapping("/{id}/decision")
  public LeaveRequestDto decide(
      @PathVariable UUID id, @Valid @RequestBody DecideLeaveRequestDto body) {
    UUID decider = SecurityUtils.requireCurrentUserId();
    return leaveRequestService.decide(id, decider, body);
  }
}
