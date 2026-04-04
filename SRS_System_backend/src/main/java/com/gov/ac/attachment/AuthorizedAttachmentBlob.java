package com.gov.ac.attachment;

/**
 * Result of an authorized read of attachment metadata; {@link #storageKey()} is for the content
 * store only and must not be sent to HTTP clients.
 */
public record AuthorizedAttachmentBlob(
    String displayName,
    String mimeType,
    long byteSize,
    String storageKey) {

  public void writeBody(AttachmentContentStore store, java.io.OutputStream out) throws java.io.IOException {
    store.copyToOutputStream(storageKey, out);
  }
}
