package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.feature.correspondence.dto.CreateCorrespondenceCommentRequestDto;
import com.gov.ac.feature.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceCommentEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.shared.notification.service.NotificationService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceCommentRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrespondenceCommentService {

  private final CorrespondenceRepository correspondenceRepository;
  private final CorrespondenceCommentRepository correspondenceCommentRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final CorrespondenceDetailMapper correspondenceDetailMapper;
  private final NotificationService notificationService;

  @Transactional
  public CorrespondenceCommentDetailDto addComment(
      UUID correspondenceId, UUID authorUserId, CreateCorrespondenceCommentRequestDto request) {
    AppUserEntity author =
        appUserRepository
            .findByIdAndDeletedAtIsNull(authorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot add comments"));
    if (!Boolean.TRUE.equals(author.getActive())) {
      throw new ForbiddenException("You cannot add comments");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    correspondenceViewAuthorization.assertCanView(author, correspondence);

    CorrespondenceCommentEntity parent = null;
    if (request.parentCommentId() != null) {
      parent =
          correspondenceCommentRepository
              .findByIdAndCorrespondence_IdAndDeletedAtIsNull(
                  request.parentCommentId(), correspondenceId)
              .orElseThrow(() -> new BadRequestException("Parent comment not found"));
    }

    CorrespondenceCommentEntity row = new CorrespondenceCommentEntity();
    row.setCorrespondence(correspondence);
    row.setAuthor(author);
    row.setBody(request.body().trim());
    row.setParentComment(parent);
    row.setCreatedBy(authorUserId);
    row.setUpdatedBy(authorUserId);
    row = correspondenceCommentRepository.save(row);

    notificationService.notifyCommentAdded(correspondence, author);

    return correspondenceDetailMapper.toCommentDto(row);
  }
}
