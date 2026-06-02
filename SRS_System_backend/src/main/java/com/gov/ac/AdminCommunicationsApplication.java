package com.gov.ac;

import com.gov.ac.feature.attachment.crypto.AttachmentEncryptionProperties;
import com.gov.ac.feature.attachment.download.AttachmentDownloadTokenProperties;
import com.gov.ac.feature.attachment.service.AttachmentStorageProperties;
import com.gov.ac.feature.attachment.signature.SignatureProperties;
import com.gov.ac.feature.attachment.verification.AttachmentVerificationProperties;
import com.gov.ac.feature.notification.channel.NotificationDispatchProperties;
import com.gov.ac.feature.notification.channel.NotificationRoutingProperties;
import com.gov.ac.feature.notification.channel.NotificationTeamsProperties;
import com.gov.ac.feature.notification.channel.NotificationWebhookProperties;
import com.gov.ac.feature.retention.RetentionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
  AttachmentStorageProperties.class,
  AttachmentEncryptionProperties.class,
  AttachmentDownloadTokenProperties.class,
  SignatureProperties.class,
  AttachmentVerificationProperties.class,
  RetentionProperties.class,
  NotificationDispatchProperties.class,
  NotificationRoutingProperties.class,
  NotificationWebhookProperties.class,
  NotificationTeamsProperties.class
})
public class AdminCommunicationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdminCommunicationsApplication.class, args);
  }
}
