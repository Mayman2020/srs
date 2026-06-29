package com.gov.ac.feature.registration.dto;

import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateFormDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Registration-desk intake wraps create payload with desk-specific routing. */
@Getter
@Setter
public class RegistrationDeskIntakeRequestDto extends CorrespondenceCreateFormDto {

  /** {@code INBOUND} or {@code OUTBOUND}. */
  @NotBlank
  @Size(max = 16)
  private String deskMode;

  /** Departments to hand off to after registration (persisted as recipients). */
  @Valid @Size(max = 50)
  private List<Long> handoffDepartmentIds;
}
