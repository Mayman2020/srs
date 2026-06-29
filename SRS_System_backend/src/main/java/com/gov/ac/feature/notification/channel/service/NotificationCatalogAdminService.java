package com.gov.ac.feature.notification.channel.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.notification.channel.dto.NotificationCatalogAdminDto;
import com.gov.ac.feature.notification.channel.dto.NotificationCatalogAdminDto.NotificationCatalogAdminItemDto;
import com.gov.ac.feature.notification.channel.dto.UpsertNotificationCatalogItemRequestDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCatalogAdminService {

  private final DataSource dataSource;

  @Transactional(readOnly = true)
  public NotificationCatalogAdminDto loadAdmin() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    return new NotificationCatalogAdminDto(
        queryItems(jdbc, "notification_event_type"), queryItems(jdbc, "notification_channel"));
  }

  @Transactional
  public NotificationCatalogAdminItemDto upsertEventType(
      UUID actorId, UpsertNotificationCatalogItemRequestDto body) {
    return upsert(actorId, "notification_event_type", body);
  }

  @Transactional
  public NotificationCatalogAdminItemDto upsertChannel(
      UUID actorId, UpsertNotificationCatalogItemRequestDto body) {
    return upsert(actorId, "notification_channel", body);
  }

  @Transactional
  public void deleteEventType(UUID actorId, String code) {
    softDelete(actorId, "notification_event_type", code);
  }

  @Transactional
  public void deleteChannel(UUID actorId, String code) {
    softDelete(actorId, "notification_channel", code);
  }

  private NotificationCatalogAdminItemDto upsert(
      UUID actorId, String table, UpsertNotificationCatalogItemRequestDto body) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    String code = body.code().trim().toUpperCase();
    Integer existing =
        jdbc.query(
            "select 1 from srs_system." + table + " where code = ? and deleted_at is null limit 1",
            rs -> rs.next() ? 1 : null,
            code);
    Instant now = Instant.now();
    if (existing == null) {
      jdbc.update(
          "insert into srs_system."
              + table
              + " (code, name_ar, name_en, sort_order, is_active, created_at, created_by, updated_at, updated_by) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          code,
          body.nameAr().trim(),
          body.nameEn().trim(),
          body.sortOrder(),
          body.active(),
          Timestamp.from(now),
          actorId,
          Timestamp.from(now),
          actorId);
    } else {
      int updated =
          jdbc.update(
              "update srs_system."
                  + table
                  + " set name_ar = ?, name_en = ?, sort_order = ?, is_active = ?, updated_at = ?, updated_by = ? "
                  + "where code = ? and deleted_at is null",
              body.nameAr().trim(),
              body.nameEn().trim(),
              body.sortOrder(),
              body.active(),
              Timestamp.from(now),
              actorId,
              code);
      if (updated == 0) {
        throw new NotFoundException("Catalog item not found");
      }
    }
    return new NotificationCatalogAdminItemDto(
        code, body.nameEn().trim(), body.nameAr().trim(), body.sortOrder(), body.active());
  }

  private void softDelete(UUID actorId, String table, String code) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    int updated =
        jdbc.update(
            "update srs_system."
                + table
                + " set deleted_at = ?, deleted_by = ?, is_active = false, updated_at = ?, updated_by = ? "
                + "where code = ? and deleted_at is null",
            Timestamp.from(Instant.now()),
            actorId,
            Timestamp.from(Instant.now()),
            actorId,
            code.trim().toUpperCase());
    if (updated == 0) {
      throw new BadRequestException("Catalog item not found or already deleted");
    }
  }

  private static List<NotificationCatalogAdminItemDto> queryItems(JdbcTemplate jdbc, String table) {
    return jdbc.query(
        "select code, name_en, name_ar, sort_order, is_active from srs_system."
            + table
            + " where deleted_at is null order by sort_order asc, code asc",
        (rs, i) ->
            new NotificationCatalogAdminItemDto(
                rs.getString("code"),
                rs.getString("name_en"),
                rs.getString("name_ar"),
                rs.getInt("sort_order"),
                rs.getBoolean("is_active")));
  }
}
