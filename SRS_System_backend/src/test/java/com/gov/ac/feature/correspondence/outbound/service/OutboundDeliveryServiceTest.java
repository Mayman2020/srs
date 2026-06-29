package com.gov.ac.feature.correspondence.outbound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.outbound.dto.UpsertOutboundDeliveryRequestDto;
import com.gov.ac.feature.correspondence.outbound.entity.CorrespondenceOutboundDeliveryEntity;
import com.gov.ac.feature.correspondence.outbound.repository.CorrespondenceOutboundDeliveryRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboundDeliveryServiceTest {

  @Mock private CorrespondenceOutboundDeliveryRepository deliveryRepository;
  @Mock private CorrespondenceRepository correspondenceRepository;

  @InjectMocks private OutboundDeliveryService service;

  @Test
  void listByCorrespondence_returnsMappedRows() {
    UUID correspondenceId = UUID.randomUUID();
    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    correspondence.setReferenceNumber("OUT-1");
    correspondence.setSubject("Subject");

    CorrespondenceOutboundDeliveryEntity row = new CorrespondenceOutboundDeliveryEntity();
    row.setId(1L);
    row.setCorrespondence(correspondence);
    row.setChannelCode("EMAIL");
    row.setStatusCode("DELIVERED");
    row.setUpdatedAt(Instant.now());

    when(deliveryRepository.findActiveByCorrespondenceId(correspondenceId)).thenReturn(List.of(row));

    var result = service.list(correspondenceId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).channelCode()).isEqualTo("EMAIL");
    assertThat(result.get(0).correspondenceId()).isEqualTo(correspondenceId);
  }

  @Test
  void create_rejectsUnknownCorrespondence() {
    UUID correspondenceId = UUID.randomUUID();
    when(correspondenceRepository.findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId))
        .thenReturn(Optional.empty());

    var body =
        new UpsertOutboundDeliveryRequestDto(
            correspondenceId, "EMAIL", "PENDING", null, null, null, null, null);

    assertThatThrownBy(() -> service.create(UUID.randomUUID(), body))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void delete_softDeletesRow() {
    UUID actor = UUID.randomUUID();
    CorrespondenceOutboundDeliveryEntity row = new CorrespondenceOutboundDeliveryEntity();
    row.setId(9L);
    row.setCorrespondence(new CorrespondenceEntity());

    when(deliveryRepository.findActiveById(9L)).thenReturn(Optional.of(row));
    when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.delete(actor, 9L);

    assertThat(row.getDeletedAt()).isNotNull();
    assertThat(row.getDeletedBy()).isEqualTo(actor);
    verify(deliveryRepository).save(row);
  }
}
