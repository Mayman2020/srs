package com.gov.ac.feature.correspondence.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateUserRecipientFormDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientKindEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceUserRecipientEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientKindRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceUserRecipientRepository;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class CorrespondenceCreateRecipientSupport {

  private final CorrespondenceRecipientRepository correspondenceRecipientRepository;
  private final CorrespondenceUserRecipientRepository correspondenceUserRecipientRepository;
  private final CorrespondenceRecipientKindRepository correspondenceRecipientKindRepository;
  private final DepartmentRepository departmentRepository;
  private final AppUserRepository appUserRepository;

  void persistAfterCreate(UUID actorUserId, CorrespondenceEntity correspondence, CorrespondenceCreateFormDto form) {
    if (!CollectionUtils.isEmpty(form.getRecipientDepartmentIds())) {
      Set<Long> seen = new HashSet<>();
      for (Long deptId : form.getRecipientDepartmentIds()) {
        if (deptId == null || !seen.add(deptId)) {
          continue;
        }
        addDepartmentRecipient(actorUserId, correspondence, deptId);
      }
    }
    if (!CollectionUtils.isEmpty(form.getCcDepartmentIds())) {
      Set<Long> seen = new HashSet<>();
      for (Long deptId : form.getCcDepartmentIds()) {
        if (deptId == null || !seen.add(deptId)) {
          continue;
        }
        if (correspondenceRecipientRepository.existsActivePair(correspondence.getId(), deptId)) {
          continue;
        }
        addDepartmentRecipient(actorUserId, correspondence, deptId);
      }
    }
    if (!CollectionUtils.isEmpty(form.getUserRecipients())) {
      for (CorrespondenceCreateUserRecipientFormDto row : form.getUserRecipients()) {
        if (row == null || row.recipientUserId() == null) {
          continue;
        }
        addUserRecipient(actorUserId, correspondence, row);
      }
    }
  }

  private void addDepartmentRecipient(UUID actorUserId, CorrespondenceEntity correspondence, Long deptId) {
    if (correspondenceRecipientRepository.existsActivePair(correspondence.getId(), deptId)) {
      return;
    }
    DepartmentEntity dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(deptId)
            .orElseThrow(() -> new BadRequestException("Unknown or deleted department"));
    CorrespondenceRecipientEntity row = new CorrespondenceRecipientEntity();
    row.setCorrespondence(correspondence);
    row.setDepartment(dept);
    row.setCreatedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    correspondenceRecipientRepository.save(row);
  }

  private void addUserRecipient(
      UUID actorUserId, CorrespondenceEntity correspondence, CorrespondenceCreateUserRecipientFormDto row) {
    CorrespondenceRecipientKindEntity kind =
        correspondenceRecipientKindRepository
            .findActiveByCode(row.recipientKindCode().trim())
            .orElseThrow(() -> new BadRequestException("Unknown recipient kind"));
    AppUserEntity recipient =
        appUserRepository
            .findByIdAndDeletedAtIsNull(row.recipientUserId())
            .filter(u -> Boolean.TRUE.equals(u.getActive()))
            .orElseThrow(() -> new BadRequestException("Unknown or inactive user"));
    if (correspondenceUserRecipientRepository.existsActiveTriple(
        correspondence.getId(), row.recipientUserId(), kind.getId())) {
      return;
    }
    CorrespondenceUserRecipientEntity entity = new CorrespondenceUserRecipientEntity();
    entity.setCorrespondence(correspondence);
    entity.setRecipientUser(recipient);
    entity.setRecipientKind(kind);
    entity.setCreatedBy(actorUserId);
    entity.setUpdatedBy(actorUserId);
    correspondenceUserRecipientRepository.save(entity);
  }
}
