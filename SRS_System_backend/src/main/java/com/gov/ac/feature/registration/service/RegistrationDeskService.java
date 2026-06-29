package com.gov.ac.feature.registration.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreatedResponseDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.feature.registration.dto.RegistrationDeskIntakeRequestDto;
import com.gov.ac.feature.registration.dto.RegistrationDeskIntakeResponseDto;
import com.gov.ac.feature.registration.dto.RegistrationDeskRowDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationDeskService {

  private final CorrespondenceCreateService correspondenceCreateService;
  private final CorrespondenceRepository correspondenceRepository;

  @Transactional
  public RegistrationDeskIntakeResponseDto intake(UUID actorUserId, RegistrationDeskIntakeRequestDto body) {
    String mode = normalizeMode(body.getDeskMode());
    body.setCorrespondenceTypeCode(mode.equals("INBOUND") ? "INBOUND" : "OUTBOUND");
    if (!CollectionUtils.isEmpty(body.getHandoffDepartmentIds())) {
      body.setRecipientDepartmentIds(body.getHandoffDepartmentIds());
      if (body.getOwnerDepartmentId() == null) {
        body.setOwnerDepartmentId(body.getHandoffDepartmentIds().get(0));
      }
    }
    CorrespondenceCreatedResponseDto created = correspondenceCreateService.create(actorUserId, body);
    String barcode =
        StringUtils.hasText(created.getBarcodeValue())
            ? created.getBarcodeValue()
            : created.getReferenceNumber();
    return new RegistrationDeskIntakeResponseDto(
        created.getId(), created.getReferenceNumber(), barcode, mode);
  }

  @Transactional(readOnly = true)
  public List<RegistrationDeskRowDto> todayRegister(String deskMode) {
    String mode = normalizeMode(deskMode);
    String typeCode = mode.equals("INBOUND") ? "INBOUND" : "OUTBOUND";
    Instant start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant end = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return correspondenceRepository
        .findAll(
            (root, q, cb) ->
                cb.and(
                    cb.isNull(root.get("deletedAt")),
                    cb.equal(root.join("correspondenceType").get("code"), typeCode),
                    cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                    cb.lessThan(root.get("createdAt"), end)))
        .stream()
        .map(c -> toRow(c, mode))
        .toList();
  }

  private static RegistrationDeskRowDto toRow(CorrespondenceEntity c, String mode) {
    return new RegistrationDeskRowDto(
        c.getId(),
        c.getReferenceNumber(),
        c.getBarcodeValue(),
        c.getSubject(),
        c.getCorrespondenceType() != null ? c.getCorrespondenceType().getCode() : null,
        mode,
        c.getCreatedAt());
  }

  private static String normalizeMode(String deskMode) {
    if (!StringUtils.hasText(deskMode)) {
      throw new BadRequestException("deskMode is required");
    }
    String mode = deskMode.trim().toUpperCase(Locale.ROOT);
    if (!mode.equals("INBOUND") && !mode.equals("OUTBOUND")) {
      throw new BadRequestException("deskMode must be INBOUND or OUTBOUND");
    }
    return mode;
  }
}
