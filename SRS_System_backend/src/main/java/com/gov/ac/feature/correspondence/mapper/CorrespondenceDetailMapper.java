package com.gov.ac.feature.correspondence.mapper;

import com.gov.ac.feature.correspondence.dto.AttachmentVersionDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCommentDetailDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDetailResponseDto;
import com.gov.ac.feature.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceTimelineEntryDto;
import com.gov.ac.feature.correspondence.dto.DepartmentSummaryDto;
import com.gov.ac.feature.correspondence.dto.LookupLabelDto;
import com.gov.ac.feature.correspondence.dto.OrganizationSummaryDto;
import com.gov.ac.feature.correspondence.dto.UserSummaryDto;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceCommentEntity;
import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceDetailMapper {

  public CorrespondenceDetailResponseDto toResponse(
      CorrespondenceEntity c,
      List<AttachmentEntity> attachments,
      Map<Long, List<AttachmentVersionEntity>> versionsByAttachmentId,
      List<CorrespondenceCommentEntity> comments,
      List<WorkflowHistoryEntity> historyRows,
      List<WorkflowActionAvailableDto> availableWorkflowActions,
      boolean cancelAllowed,
      CorrespondenceReadReceiptDto myReadReceipt,
      boolean acknowledgementSupported) {

    List<CorrespondenceAttachmentDetailDto> attachmentDtos = new ArrayList<>();
    for (AttachmentEntity a : attachments) {
      List<AttachmentVersionEntity> vlist =
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

    return CorrespondenceDetailResponseDto.builder()
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
        .replyDraftHtml(c.getReplyDraftHtml())
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
        .workflowRouteMode(
            c.getWorkflowRouteMode() != null ? c.getWorkflowRouteMode() : "AUTO")
        .serviceWorkflowRouteId(
            c.getServiceWorkflowRoute() != null ? c.getServiceWorkflowRoute().getId() : null)
        .workflowProcessDefinitionKey(workflowProcessKey(c.getServiceWorkflowRoute()))
        .supplyTransaction(Boolean.TRUE.equals(c.getSupplyTransaction()))
        .beneficiaryName(c.getBeneficiaryName())
        .beneficiaryOrganization(c.getBeneficiaryOrganization())
        .beneficiaryIdentifier(c.getBeneficiaryIdentifier())
        .attachments(attachmentDtos)
        .timeline(timeline)
        .comments(commentDtos)
        .availableWorkflowActions(availableWorkflowActions != null ? availableWorkflowActions : List.of())
        .cancelAllowed(cancelAllowed)
        .myReadReceipt(myReadReceipt)
        .acknowledgementSupported(acknowledgementSupported)
        .build();
  }

  public CorrespondenceAttachmentDetailDto toAttachmentDetail(
      AttachmentEntity a, List<AttachmentVersionEntity> versions) {
    List<AttachmentVersionDto> versionDtos = versions.stream().map(this::toVersionDto).toList();
    return CorrespondenceAttachmentDetailDto.builder()
        .id(a.getId())
        .displayName(a.getDisplayName())
        .active(Boolean.TRUE.equals(a.getActive()))
        .currentVersionId(a.getCurrentVersionId())
        .contentType(toContentTypeLabel(a.getContentType()))
        .versions(versionDtos)
        .build();
  }

  private AttachmentVersionDto toVersionDto(AttachmentVersionEntity v) {
    return AttachmentVersionDto.builder()
        .id(v.getId())
        .versionNumber(v.getVersionNumber())
        .byteSize(v.getByteSize())
        .mimeType(v.getMimeType())
        .checksumSha256(v.getChecksumSha256())
        .createdAt(v.getCreatedAt())
        .build();
  }

  public CorrespondenceCommentDetailDto toCommentDto(CorrespondenceCommentEntity cc) {
    return CorrespondenceCommentDetailDto.builder()
        .id(cc.getId())
        .body(cc.getBody())
        .createdAt(cc.getCreatedAt())
        .parentCommentId(cc.getParentComment() != null ? cc.getParentComment().getId() : null)
        .author(toUserSummary(cc.getAuthor()))
        .build();
  }

  private CorrespondenceTimelineEntryDto toTimelineEntry(WorkflowHistoryEntity h) {
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

  private static UserSummaryDto toUserSummary(AppUserEntity u) {
    return UserSummaryDto.builder()
        .id(u.getId())
        .username(u.getUsername())
        .fullNameAr(u.getFullNameAr())
        .fullNameEn(u.getFullNameEn())
        .build();
  }

  private static LookupLabelDto label(CorrespondenceTypeEntity t) {
    return LookupLabelDto.builder()
        .code(t.getCode())
        .nameAr(t.getNameAr())
        .nameEn(t.getNameEn())
        .build();
  }

  private static LookupLabelDto label(CorrespondenceStatusEntity s) {
    return LookupLabelDto.builder()
        .code(s.getCode())
        .nameAr(s.getNameAr())
        .nameEn(s.getNameEn())
        .uiVariant(s.getUiVariant())
        .build();
  }

  private static LookupLabelDto label(PriorityEntity p) {
    return LookupLabelDto.builder()
        .code(p.getCode())
        .nameAr(p.getNameAr())
        .nameEn(p.getNameEn())
        .build();
  }

  private static LookupLabelDto label(ConfidentialityEntity c) {
    return LookupLabelDto.builder()
        .code(c.getCode())
        .nameAr(c.getNameAr())
        .nameEn(c.getNameEn())
        .requiresClearance(c.getRequiresClearance())
        .build();
  }

  private static String workflowProcessKey(ServiceWorkflowRouteEntity r) {
    return r != null ? r.getProcessDefinitionKey() : null;
  }

  private static LookupLabelDto label(ClassificationEntity c) {
    return LookupLabelDto.builder()
        .code(c.getCode())
        .nameAr(c.getNameAr())
        .nameEn(c.getNameEn())
        .build();
  }

  private static LookupLabelDto toContentTypeLabel(AttachmentContentTypeEntity ct) {
    if (ct == null) {
      return null;
    }
    return LookupLabelDto.builder()
        .code(ct.getCode())
        .nameAr(ct.getNameAr())
        .nameEn(ct.getNameEn())
        .build();
  }

  private static OrganizationSummaryDto toOrganization(OrganizationEntity o) {
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

  private static DepartmentSummaryDto toDepartment(DepartmentEntity d) {
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
