package com.gov.ac.feature.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreatedResponseDto;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.service.CorrespondenceCreateService;
import com.gov.ac.feature.registration.dto.RegistrationDeskIntakeRequestDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationDeskServiceTest {

  @Mock private CorrespondenceCreateService correspondenceCreateService;
  @Mock private CorrespondenceRepository correspondenceRepository;

  @InjectMocks private RegistrationDeskService service;

  @Test
  void intake_mapsInboundTypeAndHandoffDepartments() {
    UUID actor = UUID.randomUUID();
    UUID createdId = UUID.randomUUID();
    RegistrationDeskIntakeRequestDto body = new RegistrationDeskIntakeRequestDto();
    body.setDeskMode("INBOUND");
    body.setSubject("Test subject");
    body.setHandoffDepartmentIds(List.of(10L, 20L));

    when(correspondenceCreateService.create(eq(actor), any()))
        .thenReturn(
            CorrespondenceCreatedResponseDto.builder()
                .id(createdId)
                .referenceNumber("IN-2026-001")
                .barcodeValue("BC-001")
                .correspondenceTypeCode("INBOUND")
                .correspondenceStatusCode("NEW")
                .createdAt(Instant.now())
                .build());

    var result = service.intake(actor, body);

    assertThat(result.id()).isEqualTo(createdId);
    assertThat(result.barcodeValue()).isEqualTo("BC-001");
    assertThat(result.deskMode()).isEqualTo("INBOUND");

    ArgumentCaptor<RegistrationDeskIntakeRequestDto> captor =
        ArgumentCaptor.forClass(RegistrationDeskIntakeRequestDto.class);
    verify(correspondenceCreateService).create(eq(actor), captor.capture());
    assertThat(captor.getValue().getCorrespondenceTypeCode()).isEqualTo("INBOUND");
    assertThat(captor.getValue().getRecipientDepartmentIds()).containsExactly(10L, 20L);
    assertThat(captor.getValue().getOwnerDepartmentId()).isEqualTo(10L);
  }

  @Test
  void intake_rejectsInvalidDeskMode() {
    RegistrationDeskIntakeRequestDto body = new RegistrationDeskIntakeRequestDto();
    body.setDeskMode("INVALID");

    assertThatThrownBy(() -> service.intake(UUID.randomUUID(), body))
        .isInstanceOf(BadRequestException.class);
  }
}
