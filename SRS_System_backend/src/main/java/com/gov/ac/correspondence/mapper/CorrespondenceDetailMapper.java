package com.gov.ac.correspondence.mapper;

import com.gov.ac.correspondence.dto.AttachmentVersionDto;
import com.gov.ac.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.correspondence.dto.CorrespondenceDetailResponse;
import com.gov.ac.correspondence.dto.CorrespondenceTimelineEntryDto;
import com.gov.ac.correspondence.dto.DepartmentSummaryDto;
import com.gov.ac.correspondence.dto.LookupLabelDto;
import com.gov.ac.correspondence.dto.OrganizationSummaryDto;
import com.gov.ac.correspondence.dto.UserSummaryDto;
import com.gov.ac.domain.correspondence.Attachment;
import com.gov.ac.domain.correspondence.AttachmentVersion;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.correspondence.CorrespondenceComment;
import com.gov.ac.domain.lookup.AttachmentContentType;
import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.org.Classification;
import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.org.Organization;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.workflow.WorkflowHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceDetailMapper {

  public CorrespondenceDetailResponse toResponse(
      Correspondence c,
      List<Attachment> attachments,
      Map<Long, List<AttachmentVersion>> versionsByAttachmentId,
      List<CorrespondenceComment> comments,
      List<WorkflowHistory> historyRows) {

    List<CorrespondenceAttachmentDetailDto> attachmentDtos = new ArrayList<>();
    for (Attachment a : attachments) {
      List<AttachmentVersion> vlist =
          versionsByAttachmentId.getOrDefault(a.getId(), List.of());
      List<AttachmentVersionDto> versionDtos = vlist.stream().map(this::toVersionDto).toList();
      attachmentDtos.add(
          CorrespondenceAttachmentDetailDto.builder()
              .id(a.getId())
              .displayName(a.getDisplayName())
              .active(Boolean.TRUE.equals(a.getActive()))
              .currentVersionId(a.getCurrentVersionId())
              .contentType(toContentTypeLabel(a.getContentType()))
              .versions(versionDtos)
              .build());
    }

    List<CorrespondenceCommentDetailDto> commentDtos =
        comments.stream().map(this::toCommentDto).toList();

    List<CorrespondenceTimelineEntryDto> timeline =
        historyRows.stream().map(this::toTimelineEntry).toList();

    return CorrespondenceDetailResponse.builder()
        .id(c.getId())
        .referenceNumber(c.getReferenceNumber())
        .correspondenceType(label(c.getCorrespondenceType()))
        .correspondenceStatus(label(c.getCorrespondenceStatus()))
        .priority(label(c.getPriority()))
        .confidentiality(label(c.getConfidentiality()))
        .classification(label(c.getClassification()))
        .subject(c.getSubject())
        .description(c.getDescription())
        .bodyHtml(c.getBodyHtml())
        .senderOrganization(toOrganization(c.getSenderOrganization()))
        .recipientOrganization(toOrganization(c.getRecipientOrganization()))
        .ownerDepartment(toDepartment(c.getOwnerDepartment()))
        .externalReferenceNumber(c.getExternalReferenceNumber())
        .externalReferenceDate(c.getExternalReferenceDate())
        .dueDate(c.getDueDate())
        .barcodeValue(c.getBarcodeValue())
        .totalAttachmentBytes(c.getTotalAttachmentBytes() != null ? c.getTotalAttachmentBytes() : 0L)
        .createdAt(c.getCreatedAt())
        .updatedAt(c.getUpdatedAt())
        .attachments(attachmentDtos)
        .timeline(timeline)
        .comments(commentDtos)
        .build();
  }

  private AttachmentVersionDto toVersionDto(AttachmentVersion v) {
    return AttachmentVersionDto.builder()
        .id(v.getId())
        .versionNumber(v.getVersionNumber())
        .byteSize(v.getByteSize())
        .mimeType(v.getMimeType())
        .checksumSha256(v.getChecksumSha256())
        .createdAt(v.getCreatedAt())
        .build();
  }

  public CorrespondenceCommentDetailDto toCommentDto(CorrespondenceComment cc) {
    return CorrespondenceCommentDetailDto.builder()
        .id(cc.getId())
        .body(cc.getBody())
        .createdAt(cc.getCreatedAt())
        .parentCommentId(cc.getParentComment() != null ? cc.getParentComment().getId() : null)
        .author(toUserSummary(cc.getAuthor()))
        .build();
  }

  private CorrespondenceTimelineEntryDto toTimelineEntry(WorkflowHistory h) {
    String action =
        h.getWorkflowActionType() != null
            ? h.getWorkflowActionType().getCode()
            : h.getEventType().getCode();
    return CorrespondenceTimelineEntryDto.builder()
        .historyId(h.getId())
        .sequenceNo(h.getSequenceNo())
        .action(action)
        .eventTypeCode(h.getEventType().getCode())
        .user(h.getActor() != null ? toUserSummary(h.getActor()) : null)
        .timestamp(h.getOccurredAt())
        .comment(h.getPrimaryCommentText())
        .status(
            h.getNewCorrespondenceStatus() != null
                ? h.getNewCorrespondenceStatus().getCode()
                : null)
        .previousStatusCode(
            h.getPreviousCorrespondenceStatus() != null
                ? h.getPreviousCorrespondenceStatus().getCode()
                : null)
        .build();
  }

  private static UserSummaryDto toUserSummary(AppUser u) {
    return UserSummaryDto.builder()
        .id(u.getId())
        .username(u.getUsername())
        .fullNameAr(u.getFullNameAr())
        .fullNameEn(u.getFullNameEn())
        .build();
  }

  private static LookupLabelDto label(CorrespondenceType t) {
    return LookupLabelDto.builder()
        .code(t.getCode())
        .nameAr(t.getNameAr())
        .nameEn(t.getNameEn())
        .build();
  }

  private static LookupLabelDto label(CorrespondenceStatus s) {
    return LookupLabelDto.builder()
        .code(s.getCode())
        .nameAr(s.getNameAr())
        .nameEn(s.getNameEn())
        .build();
  }

  private static LookupLabelDto label(Priority p) {
    return LookupLabelDto.builder()
        .code(p.getCode())
        .nameAr(p.getNameAr())
        .nameEn(p.getNameEn())
        .build();
  }

  private static LookupLabelDto label(Confidentiality c) {
    return LookupLabelDto.builder()
        .code(c.getCode())
        .nameAr(c.getNameAr())
        .nameEn(c.getNameEn())
        .build();
  }

  private static LookupLabelDto label(Classification c) {
    return LookupLabelDto.builder()
        .code(c.getCode())
        .nameAr(c.getNameAr())
        .nameEn(c.getNameEn())
        .build();
  }

  private static LookupLabelDto toContentTypeLabel(AttachmentContentType ct) {
    if (ct == null) {
      return null;
    }
    return LookupLabelDto.builder()
        .code(ct.getCode())
        .nameAr(ct.getNameAr())
        .nameEn(ct.getNameEn())
        .build();
  }

  private static OrganizationSummaryDto toOrganization(Organization o) {
    if (o == null) {
      return null;
    }
    return OrganizationSummaryDto.builder()
        .id(o.getId())
        .code(o.getCode())
        .nameAr(o.getNameAr())
        .nameEn(o.getNameEn())
        .build();
  }

  private static DepartmentSummaryDto toDepartment(Department d) {
    if (d == null) {
      return null;
    }
    return DepartmentSummaryDto.builder()
        .id(d.getId())
        .code(d.getCode())
        .nameAr(d.getNameAr())
        .nameEn(d.getNameEn())
        .build();
  }
}
