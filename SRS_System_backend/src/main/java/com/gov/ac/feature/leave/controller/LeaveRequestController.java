package com.gov.ac.feature.leave.controller;

import com.gov.ac.feature.leave.service.LeaveRequestService;
import com.gov.ac.feature.leave.dto.CreateLeaveRequestDto;
import com.gov.ac.feature.leave.dto.LeaveRequestDto;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

  private final LeaveRequestService leaveRequestService;

  @PostMapping
  public LeaveRequestDto create(@Valid @RequestBody CreateLeaveRequestDto body) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return leaveRequestService.create(userId, body);
  }

  @GetMapping("/mine")
  public List<LeaveRequestDto> mine() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return leaveRequestService.listMine(userId);
  }
}
