package com.gov.ac.feature.correspondence.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.dto.AttachmentIndexEntryDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceLinkListItemDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceNonarchivedItemDto;
import com.gov.ac.feature.correspondence.dto.CreateCorrespondenceLinkRequestDto;
import com.gov.ac.feature.correspondence.dto.UpsertAttachmentIndexEntryRequestDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceNonarchivedItemRequestDto;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentIndexEntryEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceLinkEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceNonarchivedItemEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentIndexEntryRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceLinkRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceNonarchivedItemRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceGuideService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final CorrespondenceLinkRepository correspondenceLinkRepository;
  private final CorrespondenceNonarchivedItemRepository correspondenceNonarchivedItemRepository;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentIndexEntryRepository attachmentIndexEntryRepository;

  @Transactional(readOnly = true)
  public List<CorrespondenceLinkListItemDto> listLinks(UUID correspondenceId, UUID viewerId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    return correspondenceLinkRepository.listActiveForCorrespondence(correspondenceId).stream()
        .map(
            l ->
                new CorrespondenceLinkListItemDto(
                    l.getId(),
                    l.getLinkedCorrespondence().getId(),
                    l.getLinkedCorrespondence().getReferenceNumber(),
                    l.getLinkedCorrespondence().getSubject(),
                    l.getLinkKind(),
                    l.getNotes()))
        .toList();
  }

  @Transactional
  public CorrespondenceLinkListItemDto addLink(
      UUID correspondenceId, UUID viewerId, CreateCorrespondenceLinkRequestDto req) {
    CorrespondenceEntity base = assertCorrespondenceView(correspondenceId, viewerId);
    if (req.linkedCorrespondenceId().equals(correspondenceId)) {
      throw new BadRequestException("Cannot link a correspondence to itself");
    }
    if (correspondenceLinkRepository.existsActivePair(correspondenceId, req.linkedCorrespondenceId())) {
      throw new BadRequestException("This link already exists");
    }
    CorrespondenceEntity linked =
        correspondenceRepository
            .findDetailGraphByIdAndDeletedAtIsNull(req.linkedCorrespondenceId())
            .orElseThrow(() -> new NotFoundException("Linked correspondence not found"));
    AppUserEntity viewer = loadActiveViewer(viewerId);
    correspondenceViewAuthorization.assertCanView(viewer, linked);

    CorrespondenceLinkEntity row = new CorrespondenceLinkEntity();
    row.setCorrespondence(base);
    row.setLinkedCorrespondence(linked);
    row.setLinkKind(
        req.linkKind() != null && !req.linkKind().isBlank() ? req.linkKind().trim() : "RELATED");
    row.setNotes(req.notes());
    row.setCreatedBy(viewerId);
    CorrespondenceLinkEntity saved = correspondenceLinkRepository.save(row);
    return new CorrespondenceLinkListItemDto(
        saved.getId(),
        linked.getId(),
        linked.getReferenceNumber(),
        linked.getSubject(),
        saved.getLinkKind(),
        saved.getNotes());
  }

  @Transactional
  public void deleteLink(UUID correspondenceId, UUID viewerId, long linkId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    CorrespondenceLinkEntity link =
        correspondenceLinkRepository
            .findActiveByIdAndCorrespondence(linkId, correspondenceId)
            .orElseThrow(() -> new NotFoundException("Link not found"));
    link.setDeletedAt(Instant.now());
  }

  @Transactional(readOnly = true)
  public List<CorrespondenceNonarchivedItemDto> listNonarchived(
      UUID correspondenceId, UUID viewerId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    return correspondenceNonarchivedItemRepository.listActiveForCorrespondence(correspondenceId).stream()
        .map(
            i ->
                new CorrespondenceNonarchivedItemDto(
                    i.getId(),
                    i.getItemType(),
                    i.getDescriptionText(),
                    i.getQuantity(),
                    i.getSortOrder()))
        .toList();
  }

  @Transactional
  public CorrespondenceNonarchivedItemDto addNonarchived(
      UUID correspondenceId, UUID viewerId, UpsertCorrespondenceNonarchivedItemRequestDto req) {
    CorrespondenceEntity base = assertCorrespondenceView(correspondenceId, viewerId);
    CorrespondenceNonarchivedItemEntity row = new CorrespondenceNonarchivedItemEntity();
    row.setCorrespondence(base);
    row.setItemType(req.itemType().trim());
    row.setDescriptionText(req.descriptionText());
    row.setQuantity(req.quantity());
    row.setSortOrder(req.sortOrder());
    row.setCreatedBy(viewerId);
    CorrespondenceNonarchivedItemEntity saved = correspondenceNonarchivedItemRepository.save(row);
    return new CorrespondenceNonarchivedItemDto(
        saved.getId(),
        saved.getItemType(),
        saved.getDescriptionText(),
        saved.getQuantity(),
        saved.getSortOrder());
  }

  @Transactional
  public CorrespondenceNonarchivedItemDto updateNonarchived(
      UUID correspondenceId,
      UUID viewerId,
      long itemId,
      UpsertCorrespondenceNonarchivedItemRequestDto req) {
    assertCorrespondenceView(correspondenceId, viewerId);
    CorrespondenceNonarchivedItemEntity row =
        correspondenceNonarchivedItemRepository
            .findActiveByIdAndCorrespondence(itemId, correspondenceId)
            .orElseThrow(() -> new NotFoundException("Item not found"));
    row.setItemType(req.itemType().trim());
    row.setDescriptionText(req.descriptionText());
    row.setQuantity(req.quantity());
    row.setSortOrder(req.sortOrder());
    return new CorrespondenceNonarchivedItemDto(
        row.getId(),
        row.getItemType(),
        row.getDescriptionText(),
        row.getQuantity(),
        row.getSortOrder());
  }

  @Transactional
  public void deleteNonarchived(UUID correspondenceId, UUID viewerId, long itemId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    CorrespondenceNonarchivedItemEntity row =
        correspondenceNonarchivedItemRepository
            .findActiveByIdAndCorrespondence(itemId, correspondenceId)
            .orElseThrow(() -> new NotFoundException("Item not found"));
    row.setDeletedAt(Instant.now());
  }

  @Transactional(readOnly = true)
  public List<AttachmentIndexEntryDto> listIndexEntries(Long attachmentId, UUID viewerId) {
    AttachmentEntity att = loadAttachmentForView(attachmentId, viewerId);
    return attachmentIndexEntryRepository.listActiveForAttachment(att.getId()).stream()
        .map(
            e ->
                new AttachmentIndexEntryDto(
                    e.getId(), e.getPageFrom(), e.getPageTo(), e.getSubjectText(), e.getSortOrder()))
        .toList();
  }

  @Transactional
  public AttachmentIndexEntryDto addIndexEntry(
      Long attachmentId, UUID viewerId, UpsertAttachmentIndexEntryRequestDto req) {
    AttachmentEntity att = loadAttachmentForView(attachmentId, viewerId);
    validatePageRange(req.pageFrom(), req.pageTo());
    AttachmentIndexEntryEntity row = new AttachmentIndexEntryEntity();
    row.setAttachment(att);
    row.setPageFrom(req.pageFrom());
    row.setPageTo(req.pageTo());
    row.setSubjectText(req.subjectText());
    row.setSortOrder(req.sortOrder());
    row.setCreatedBy(viewerId);
    AttachmentIndexEntryEntity saved = attachmentIndexEntryRepository.save(row);
    return new AttachmentIndexEntryDto(
        saved.getId(),
        saved.getPageFrom(),
        saved.getPageTo(),
        saved.getSubjectText(),
        saved.getSortOrder());
  }

  @Transactional
  public AttachmentIndexEntryDto updateIndexEntry(
      Long attachmentId, UUID viewerId, long entryId, UpsertAttachmentIndexEntryRequestDto req) {
    loadAttachmentForView(attachmentId, viewerId);
    validatePageRange(req.pageFrom(), req.pageTo());
    AttachmentIndexEntryEntity row =
        attachmentIndexEntryRepository
            .findActiveByIdAndAttachment(entryId, attachmentId)
            .orElseThrow(() -> new NotFoundException("Index entry not found"));
    row.setPageFrom(req.pageFrom());
    row.setPageTo(req.pageTo());
    row.setSubjectText(req.subjectText());
    row.setSortOrder(req.sortOrder());
    return new AttachmentIndexEntryDto(
        row.getId(),
        row.getPageFrom(),
        row.getPageTo(),
        row.getSubjectText(),
        row.getSortOrder());
  }

  @Transactional
  public void deleteIndexEntry(Long attachmentId, UUID viewerId, long entryId) {
    loadAttachmentForView(attachmentId, viewerId);
    AttachmentIndexEntryEntity row =
        attachmentIndexEntryRepository
            .findActiveByIdAndAttachment(entryId, attachmentId)
            .orElseThrow(() -> new NotFoundException("Index entry not found"));
    row.setDeletedAt(Instant.now());
  }

  private CorrespondenceEntity assertCorrespondenceView(UUID correspondenceId, UUID viewerId) {
    CorrespondenceEntity c =
        correspondenceRepository
            .findDetailGraphByIdAndDeletedAtIsNull(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    AppUserEntity viewer = loadActiveViewer(viewerId);
    correspondenceViewAuthorization.assertCanView(viewer, c);
    return c;
  }

  private AttachmentEntity loadAttachmentForView(Long attachmentId, UUID viewerId) {
    AttachmentEntity att =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("AttachmentEntity not found"));
    CorrespondenceEntity c = att.getCorrespondence();
    AppUserEntity viewer = loadActiveViewer(viewerId);
    correspondenceViewAuthorization.assertCanView(viewer, c);
    return att;
  }

  private AppUserEntity loadActiveViewer(UUID viewerId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn("Guide data denied: unknown viewer userId={}", viewerId);
                  return new ForbiddenException("You do not have access to this correspondence");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this correspondence");
    }
    return viewer;
  }

  private static void validatePageRange(Integer from, Integer to) {
    if (from != null && to != null && from > to) {
      throw new BadRequestException("pageFrom must be less than or equal to pageTo");
    }
  }
}
