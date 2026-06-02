package com.gov.ac.feature.notification.channel.provider;

import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.TerminalNotificationDispatchException;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.dispatch.service.OutboundMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannelProvider implements NotificationChannelProvider {

  private final OutboundMailService outboundMailService;

  @Override
  public String code() {
    return NotificationOutboxService.CHANNEL_EMAIL;
  }

  @Override
  public boolean supports(NotificationOutboxEntity row) {
    return NotificationOutboxService.CHANNEL_EMAIL.equalsIgnoreCase(row.getChannelCode());
  }

  @Override
  public void dispatch(NotificationOutboxEntity row) throws Exception {
    String to = row.getRecipientAddress();
    if (to == null || to.isBlank()) {
      throw new TerminalNotificationDispatchException("EMAIL missing recipient_address");
    }
    String subject =
        row.getSubject() != null && !row.getSubject().isBlank()
            ? row.getSubject()
            : (row.getMessageKey() != null ? row.getMessageKey() : "Notification");
    String body =
        row.getBodyText() != null && !row.getBodyText().isBlank()
            ? row.getBodyText()
            : (row.getMessageParamsJson() != null ? row.getMessageParamsJson() : "");
    try {
      outboundMailService.sendOrThrow(to, subject, body);
    } catch (MailAuthenticationException | MailParseException ex) {
      throw new TerminalNotificationDispatchException(ex.getMessage(), ex);
    }
  }
}
