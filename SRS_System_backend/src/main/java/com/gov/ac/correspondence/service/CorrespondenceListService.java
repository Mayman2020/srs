package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.correspondence.mapper.CorrespondenceListMapper;
import com.gov.ac.correspondence.query.CorrespondenceListPageables;
import com.gov.ac.correspondence.query.CorrespondenceSpecifications;
import com.gov.ac.correspondence.security.CorrespondencePrivilegedRoleChecker;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.common.api.ForbiddenException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceListService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondencePrivilegedRoleChecker privilegedRoleChecker;
  private final CorrespondenceListMapper correspondenceListMapper;

  @Transactional(readOnly = true)
  public Page<CorrespondenceListItemDto> search(
      Pageable pageable,
      String statusCode,
      String typeCode,
      String priorityCode,
      Instant createdFrom,
      Instant createdTo,
      UUID viewerId) {
    AppUser viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn("Correspondence list denied: unknown or deleted viewer userId={}", viewerId);
                  return new ForbiddenException("You do not have access to correspondence");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      log.warn("Correspondence list denied: inactive viewer userId={}", viewerId);
      throw new ForbiddenException("You do not have access to correspondence");
    }

    boolean privileged = privilegedRoleChecker.hasPrivilegedViewRole(viewerId);
    Long deptId = viewer.getDepartment().getId();

    Pageable p = CorrespondenceListPageables.sanitize(pageable);
    var spec =
        CorrespondenceSpecifications.forList(
            privileged, deptId, statusCode, typeCode, priorityCode, createdFrom, createdTo);

    return correspondenceRepository.findAll(spec, p).map(correspondenceListMapper::toListItem);
  }
}
