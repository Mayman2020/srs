package com.gov.ac.feature.attachment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.storage")
public record AttachmentStorageProperties(
    /** Directory root; attachment_version.storage_key is resolved only under this path. */
    String root) {

  public AttachmentStorageProperties {
    root = (root == null || root.isBlank()) ? "./data/attachments" : root;
  }
}
