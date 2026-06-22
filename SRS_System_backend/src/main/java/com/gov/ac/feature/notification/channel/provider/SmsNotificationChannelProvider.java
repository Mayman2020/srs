package com.gov.ac.feature.notification.channel.provider;

import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.TerminalNotificationDispatchException;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.dispatch.service.OutboundSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationChannelProvider implements NotificationChannelProvider {

  private final OutboundSmsService outboundSmsService;

  @Override
  public String code() {
    return NotificationOutboxService.CHANNEL_SMS;
  }

  @Override
  public boolean supports(NotificationOutboxEntity row) {
    return NotificationOutboxService.CHANNEL_SMS.equalsIgnoreCase(row.getChannelCode());
  }

  @Override
  public void dispatch(NotificationOutboxEntity row) throws Exception {
    String phone = row.getRecipientAddress();
    if (phone == null || phone.isBlank()) {
      throw new TerminalNotificationDispatchException("SMS missing recipient_address");
    }
    String body =
        row.getBodyText() != null && !row.getBodyText().isBlank()
            ? row.getBodyText()
            : (row.getMessageKey() != null ? row.getMessageKey() : "Notification");
    outboundSmsService.sendOrThrow(phone, body);
  }
}
