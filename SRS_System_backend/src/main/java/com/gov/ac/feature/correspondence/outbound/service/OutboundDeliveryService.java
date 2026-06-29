package com.gov.ac.feature.correspondence.outbound.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.outbound.dto.OutboundDeliveryDto;
import com.gov.ac.feature.correspondence.outbound.dto.UpsertOutboundDeliveryRequestDto;
import com.gov.ac.feature.correspondence.outbound.entity.CorrespondenceOutboundDeliveryEntity;
import com.gov.ac.feature.correspondence.outbound.repository.CorrespondenceOutboundDeliveryRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OutboundDeliveryService {

  private final CorrespondenceOutboundDeliveryRepository deliveryRepository;
  private final CorrespondenceRepository correspondenceRepository;

  @Transactional(readOnly = true)
  public List<OutboundDeliveryDto> list(UUID correspondenceId) {
    List<CorrespondenceOutboundDeliveryEntity> rows =
        correspondenceId != null
            ? deliveryRepository.findActiveByCorrespondenceId(correspondenceId)
            : deliveryRepository.findAllActive();
    return rows.stream().map(this::toDto).toList();
  }

  @Transactional
  public OutboundDeliveryDto create(UUID actorId, UpsertOutboundDeliveryRequestDto body) {
    CorrespondenceEntity correspondence = loadCorrespondence(body.correspondenceId());
    CorrespondenceOutboundDeliveryEntity row = new CorrespondenceOutboundDeliveryEntity();
    row.setCorrespondence(correspondence);
    apply(row, body);
    row.setCreatedBy(actorId);
    row.setUpdatedBy(actorId);
    return toDto(deliveryRepository.save(row));
  }

  @Transactional
  public OutboundDeliveryDto update(UUID actorId, long id, UpsertOutboundDeliveryRequestDto body) {
    CorrespondenceOutboundDeliveryEntity row =
        deliveryRepository
            .findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Outbound delivery not found"));
    if (!row.getCorrespondence().getId().equals(body.correspondenceId())) {
      row.setCorrespondence(loadCorrespondence(body.correspondenceId()));
    }
    apply(row, body);
    row.setUpdatedBy(actorId);
    return toDto(deliveryRepository.save(row));
  }

  @Transactional
  public void delete(UUID actorId, long id) {
    CorrespondenceOutboundDeliveryEntity row =
        deliveryRepository
            .findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Outbound delivery not found"));
    row.setDeletedAt(Instant.now());
    row.setDeletedBy(actorId);
    deliveryRepository.save(row);
  }

  private void apply(CorrespondenceOutboundDeliveryEntity row, UpsertOutboundDeliveryRequestDto body) {
    row.setChannelCode(body.channelCode().trim().toUpperCase());
    row.setStatusCode(body.statusCode().trim().toUpperCase());
    row.setRecipientLabel(trimToNull(body.recipientLabel()));
    row.setProofReference(trimToNull(body.proofReference()));
    row.setNotes(trimToNull(body.notes()));
    row.setSentAt(body.sentAt());
    row.setDeliveredAt(body.deliveredAt());
  }

  private CorrespondenceEntity loadCorrespondence(UUID id) {
    return correspondenceRepository
        .findByIdAndDeletedAtIsNullWithOwnerDepartment(id)
        .orElseThrow(() -> new BadRequestException("Unknown correspondence"));
  }

  private OutboundDeliveryDto toDto(CorrespondenceOutboundDeliveryEntity row) {
    CorrespondenceEntity c = row.getCorrespondence();
    return new OutboundDeliveryDto(
        row.getId(),
        c.getId(),
        c.getReferenceNumber(),
        c.getSubject(),
        row.getChannelCode(),
        row.getStatusCode(),
        row.getRecipientLabel(),
        row.getProofReference(),
        row.getNotes(),
        row.getSentAt(),
        row.getDeliveredAt(),
        row.getUpdatedAt());
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
