package com.gov.ac;

import com.gov.ac.attachment.AttachmentStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AttachmentStorageProperties.class)
public class AdminCommunicationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdminCommunicationsApplication.class, args);
  }
}
