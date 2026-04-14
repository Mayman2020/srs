package com.gov.ac.feature.communication.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.communication.entity.CircularEntity;
import com.gov.ac.feature.communication.entity.CircularRecipientEntity;
import com.gov.ac.feature.communication.entity.CircularRecipientId;
import com.gov.ac.feature.communication.dto.CircularInboxRowDto;
import com.gov.ac.feature.communication.dto.CreateCircularRequestDto;
import com.gov.ac.feature.communication.repository.CircularRecipientRepository;
import com.gov.ac.feature.communication.repository.CircularRepository;
import com.gov.ac.feature.communication.mapper.CircularMapper;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CircularService {

  private final CircularRepository circularRepository;
  private final CircularRecipientRepository recipientRepository;

  @Transactional
  public UUID create(CreateCircularRequestDto req) {
    if (!req.broadcast() && (req.recipientUserIds() == null || req.recipientUserIds().isEmpty())) {
      throw new BadRequestException("Recipients required unless broadcast");
    }
    CircularEntity c = new CircularEntity();
    c.setTitle(req.title().trim());
    c.setBody(req.body());
    c.setCreatedBy(req.createdBy().trim());
    c.setBroadcast(req.broadcast());
    c.setId(UUID.randomUUID());
    if (!req.broadcast()) {
      for (String uid : req.recipientUserIds()) {
        CircularRecipientEntity r = new CircularRecipientEntity();
        CircularRecipientId id = new CircularRecipientId();
        id.setCircularId(c.getId());
        id.setUserId(uid.trim());
        r.setId(id);
        r.setCircular(c);
        c.getRecipients().add(r);
      }
    }
    return circularRepository.save(c).getId();
  }

  @Transactional(readOnly = true)
  public List<CircularInboxRowDto> inbox(String userId) {
    String u = userId.trim();
    List<CircularEntity> items = circularRepository.findInboxForUser(u);
    return items.stream()
        .map(
            c -> {
              boolean read =
                  recipientRepository
                      .findByIdCircularIdAndIdUserId(c.getId(), u)
                      .map(row -> row.getReadAt() != null)
                      .orElse(false);
              return CircularMapper.toInboxRow(c, read);
            })
        .toList();
  }

  @Transactional
  public void markRead(UUID circularId, String userId) {
    String u = userId.trim();
    CircularEntity c =
        circularRepository
            .findById(circularId)
            .orElseThrow(() -> new NotFoundException("CircularEntity not found"));
    if (!c.isBroadcast()) {
      CircularRecipientEntity r =
          recipientRepository
              .findByIdCircularIdAndIdUserId(circularId, u)
              .orElseThrow(() -> new ForbiddenException("Not a recipient"));
      r.setReadAt(Instant.now());
      recipientRepository.save(r);
      return;
    }
    CircularRecipientEntity r =
        recipientRepository
            .findByIdCircularIdAndIdUserId(circularId, u)
            .orElseGet(
                () -> {
                  CircularRecipientEntity nr = new CircularRecipientEntity();
                  CircularRecipientId id = new CircularRecipientId();
                  id.setCircularId(c.getId());
                  id.setUserId(u);
                  nr.setId(id);
                  nr.setCircular(c);
                  return nr;
                });
    r.setReadAt(Instant.now());
    recipientRepository.save(r);
  }
}
