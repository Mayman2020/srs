package com.gov.ac.correspondence.service;

import com.gov.ac.common.audit.UserAuditRefDto;
import com.gov.ac.common.audit.UserAuditResolutionService;
import com.gov.ac.correspondence.dto.CorrespondenceListItemDto;
import com.gov.ac.correspondence.mapper.CorrespondenceListMapper;
import com.gov.ac.correspondence.query.CorrespondenceListPageables;
import com.gov.ac.correspondence.query.CorrespondenceSpecifications;
import com.gov.ac.correspondence.security.CorrespondencePrivilegedRoleChecker;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.common.api.ForbiddenException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  private final UserAuditResolutionService userAuditResolutionService;

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

    var page = correspondenceRepository.findAll(spec, p);
    Set<UUID> actorIds = new HashSet<>();
    for (Correspondence c : page.getContent()) {
      if (c.getCreatedBy() != null) {
        actorIds.add(c.getCreatedBy());
      }
      if (c.getUpdatedBy() != null) {
        actorIds.add(c.getUpdatedBy());
      }
    }
    Map<UUID, UserAuditRefDto> actors = userAuditResolutionService.toRefMap(actorIds);
    return page.map(c -> enrichActors(correspondenceListMapper.toListItem(c), c, actors));
  }

  private static CorrespondenceListItemDto enrichActors(
      CorrespondenceListItemDto dto,
      Correspondence entity,
      Map<UUID, UserAuditRefDto> actors) {
    return dto.toBuilder()
        .createdByUser(refOrNull(actors, entity.getCreatedBy()))
        .updatedByUser(refOrNull(actors, entity.getUpdatedBy()))
        .build();
  }

  private static UserAuditRefDto refOrNull(Map<UUID, UserAuditRefDto> actors, UUID id) {
    if (id == null) {
      return null;
    }
    return actors.get(id);
  }
}
