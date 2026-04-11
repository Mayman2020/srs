package com.gov.ac.common.audit;

import com.gov.ac.persistence.AppUserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuditResolutionService {

  private final AppUserRepository appUserRepository;

  @Transactional(readOnly = true)
  public Optional<UserAuditRefDto> toRef(UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return appUserRepository.findById(userId).map(UserAuditRefDto::from);
  }

  /** Batch-load users for list/detail DTO enrichment (avoids N+1). */
  @Transactional(readOnly = true)
  public Map<UUID, UserAuditRefDto> toRefMap(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    Set<UUID> unique = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    if (unique.isEmpty()) {
      return Map.of();
    }
    return appUserRepository.findAllById(unique).stream()
        .map(UserAuditRefDto::from)
        .collect(Collectors.toMap(UserAuditRefDto::id, Function.identity()));
  }
}
