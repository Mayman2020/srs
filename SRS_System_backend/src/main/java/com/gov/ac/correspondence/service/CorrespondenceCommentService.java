package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.correspondence.dto.CreateCorrespondenceCommentRequest;
import com.gov.ac.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.correspondence.CorrespondenceComment;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.notification.NotificationService;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceCommentRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
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
      UUID correspondenceId, UUID authorUserId, CreateCorrespondenceCommentRequest request) {
    AppUser author =
        appUserRepository
            .findByIdAndDeletedAtIsNull(authorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot add comments"));
    if (!Boolean.TRUE.equals(author.getActive())) {
      throw new ForbiddenException("You cannot add comments");
    }

    Correspondence correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));
    correspondenceViewAuthorization.assertCanView(author, correspondence);

    CorrespondenceComment parent = null;
    if (request.parentCommentId() != null) {
      parent =
          correspondenceCommentRepository
              .findByIdAndCorrespondence_IdAndDeletedAtIsNull(
                  request.parentCommentId(), correspondenceId)
              .orElseThrow(() -> new BadRequestException("Parent comment not found"));
    }

    CorrespondenceComment row = new CorrespondenceComment();
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
