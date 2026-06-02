package com.gov.ac.feature.notification.channel.service;

import com.gov.ac.feature.notification.channel.dto.NotificationCatalogDto;
import com.gov.ac.feature.notification.channel.dto.NotificationCatalogDto.NotificationCatalogItemDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only catalog of notification event types and channels used by the preferences and channel
 * admin UIs. Backed by direct JDBC against the canonical seed tables so we do not have to add
 * additional JPA entities just for this read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCatalogService {

  private final DataSource dataSource;

  @Transactional(readOnly = true)
  public NotificationCatalogDto load() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    List<NotificationCatalogItemDto> eventTypes =
        safeQuery(
            jdbc,
            "select code, name_en, name_ar, sort_order from srs_system.notification_event_type "
                + "where is_active = true and deleted_at is null order by sort_order asc, code asc");
    List<NotificationCatalogItemDto> channels =
        safeQuery(
            jdbc,
            "select code, name_en, name_ar, sort_order from srs_system.notification_channel "
                + "where is_active = true and deleted_at is null order by sort_order asc, code asc");
    return new NotificationCatalogDto(eventTypes, channels);
  }

  private static List<NotificationCatalogItemDto> safeQuery(JdbcTemplate jdbc, String sql) {
    try {
      List<NotificationCatalogItemDto> rows =
          jdbc.query(
              sql,
              (rs, i) ->
                  new NotificationCatalogItemDto(
                      rs.getString("code"), rs.getString("name_en"), rs.getString("name_ar")));
      List<NotificationCatalogItemDto> mutable = new ArrayList<>(rows);
      mutable.sort(Comparator.comparing(NotificationCatalogItemDto::code));
      return Collections.unmodifiableList(mutable);
    } catch (RuntimeException ex) {
      log.warn("notification catalog query failed: {}", ex.getMessage());
      return List.of();
    }
  }
}
