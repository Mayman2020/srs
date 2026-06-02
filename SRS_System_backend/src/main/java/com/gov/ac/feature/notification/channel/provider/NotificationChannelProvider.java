package com.gov.ac.feature.notification.channel.provider;

import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;

public interface NotificationChannelProvider {

  String code();

  boolean supports(NotificationOutboxEntity row);

  void dispatch(NotificationOutboxEntity row) throws Exception;
}
