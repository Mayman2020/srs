package com.gov.ac.correspondence.web;

import com.gov.ac.correspondence.dto.CorrespondenceCreateForm;
import com.gov.ac.correspondence.dto.CorrespondenceCreatedResponse;
import com.gov.ac.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.web.NotFoundException;
import com.gov.ac.web.dto.CorrespondenceListDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/correspondence")
@RequiredArgsConstructor
public class CorrespondenceController {

  private final CorrespondenceRepository correspondenceRepository;
  private final CorrespondenceCreateService correspondenceCreateService;

  @GetMapping
  public Page<CorrespondenceListDto> page(@PageableDefault(size = 20) Pageable pageable) {
    return correspondenceRepository.findByDeletedAtIsNull(pageable).map(this::toDto);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CorrespondenceCreatedResponse create(@Valid @RequestBody CorrespondenceCreateForm form) {
    UUID userId = currentUserId();
    return correspondenceCreateService.create(userId, form);
  }

  @GetMapping("/{id}")
  public CorrespondenceListDto one(@PathVariable UUID id) {
    Correspondence c =
        correspondenceRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));
    return toDto(c);
  }

  private static UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
      throw new BadCredentialsException("Authentication required");
    }
    return id;
  }

  private CorrespondenceListDto toDto(Correspondence c) {
    return new CorrespondenceListDto(
        c.getId(),
        c.getReferenceNumber(),
        c.getSubject(),
        c.getCorrespondenceType().getCode(),
        c.getCorrespondenceStatus().getCode(),
        c.getPriority().getCode(),
        c.getCreatedAt());
  }
}
