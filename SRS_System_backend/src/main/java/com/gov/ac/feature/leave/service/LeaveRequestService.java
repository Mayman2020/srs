package com.gov.ac.feature.leave.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.leave.entity.LeaveRequestEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.leave.dto.CreateLeaveRequestDto;
import com.gov.ac.feature.leave.dto.DecideLeaveRequestDto;
import com.gov.ac.feature.leave.dto.LeaveRequestDto;
import com.gov.ac.feature.leave.mapper.LeaveRequestMapper;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.leave.repository.LeaveRequestRepository;
import com.gov.ac.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

  private final LeaveRequestRepository leaveRequestRepository;
  private final AppUserRepository appUserRepository;

  @Transactional
  public LeaveRequestDto create(UUID actorId, CreateLeaveRequestDto req) {
    if (req.endDate().isBefore(req.startDate())) {
      throw new BadRequestException("endDate must be on or after startDate");
    }
    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorId)
            .orElseThrow(() -> new BadRequestException("Unknown user"));
    LeaveRequestEntity lr = new LeaveRequestEntity();
    lr.setUser(user);
    lr.setStartDate(req.startDate());
    lr.setEndDate(req.endDate());
    lr.setReason(req.reason() != null ? req.reason().trim() : null);
    lr.setStatusCode("PENDING");
    lr.setCreatedBy(actorId);
    lr.setUpdatedBy(actorId);
    lr = leaveRequestRepository.save(lr);
    return LeaveRequestMapper.toDto(lr);
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestDto> listMine(UUID userId) {
    return leaveRequestRepository.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
        .map(LeaveRequestMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestDto> listAll() {
    return leaveRequestRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
        .map(LeaveRequestMapper::toDto)
        .toList();
  }

  @Transactional
  public LeaveRequestDto decide(UUID id, UUID deciderId, DecideLeaveRequestDto req) {
    String code = req.statusCode().trim().toUpperCase();
    if (!"APPROVED".equals(code) && !"REJECTED".equals(code)) {
      throw new BadRequestException("statusCode must be APPROVED or REJECTED");
    }
    LeaveRequestEntity lr =
        leaveRequestRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Leave request not found"));
    if (!"PENDING".equalsIgnoreCase(lr.getStatusCode())) {
      throw new BadRequestException("Leave request is not pending");
    }
    AppUserEntity decider =
        appUserRepository
            .findByIdAndDeletedAtIsNull(deciderId)
            .orElseThrow(() -> new BadRequestException("Unknown decider user"));
    lr.setStatusCode(code);
    lr.setDecidedBy(decider);
    lr.setDecidedAt(Instant.now());
    lr.setDecisionNote(req.decisionNote() != null ? req.decisionNote().trim() : null);
    lr.setUpdatedBy(deciderId);
    lr = leaveRequestRepository.save(lr);
    return LeaveRequestMapper.toDto(lr);
  }
}
