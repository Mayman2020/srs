package com.gov.ac.modules.communication.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.communication.Circular;
import com.gov.ac.domain.communication.CircularRecipient;
import com.gov.ac.domain.communication.CircularRecipientId;
import com.gov.ac.modules.communication.web.dto.CircularInboxRow;
import com.gov.ac.modules.communication.web.dto.CreateCircularRequest;
import com.gov.ac.persistence.CircularRecipientRepository;
import com.gov.ac.persistence.CircularRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
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
  public UUID create(CreateCircularRequest req) {
    if (!req.broadcast() && (req.recipientUserIds() == null || req.recipientUserIds().isEmpty())) {
      throw new BadRequestException("Recipients required unless broadcast");
    }
    Circular c = new Circular();
    c.setTitle(req.title().trim());
    c.setBody(req.body());
    c.setCreatedBy(req.createdBy().trim());
    c.setBroadcast(req.broadcast());
    c.setId(UUID.randomUUID());
    if (!req.broadcast()) {
      for (String uid : req.recipientUserIds()) {
        CircularRecipient r = new CircularRecipient();
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
  public List<CircularInboxRow> inbox(String userId) {
    String u = userId.trim();
    List<Circular> items = circularRepository.findInboxForUser(u);
    List<CircularInboxRow> out = new ArrayList<>();
    for (Circular c : items) {
      boolean read = false;
      var row = recipientRepository.findByIdCircularIdAndIdUserId(c.getId(), u);
      if (row.isPresent() && row.get().getReadAt() != null) {
        read = true;
      }
      out.add(
          new CircularInboxRow(
              c.getId(), c.getTitle(), c.getCreatedBy(), c.getCreatedAt(), c.isBroadcast(), read));
    }
    return out;
  }

  @Transactional
  public void markRead(UUID circularId, String userId) {
    String u = userId.trim();
    Circular c =
        circularRepository
            .findById(circularId)
            .orElseThrow(() -> new NotFoundException("Circular not found"));
    if (!c.isBroadcast()) {
      CircularRecipient r =
          recipientRepository
              .findByIdCircularIdAndIdUserId(circularId, u)
              .orElseThrow(() -> new ForbiddenException("Not a recipient"));
      r.setReadAt(Instant.now());
      recipientRepository.save(r);
      return;
    }
    CircularRecipient r =
        recipientRepository
            .findByIdCircularIdAndIdUserId(circularId, u)
            .orElseGet(
                () -> {
                  CircularRecipient nr = new CircularRecipient();
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
