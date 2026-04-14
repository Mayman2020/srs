package com.gov.ac.feature.attachment.service;

import java.io.IOException;
import java.io.OutputStream;

/** Resolves internal {@code storage_key} values to bytes; keys are never exposed to clients. */
public interface AttachmentContentStore {

  void copyToOutputStream(String storageKey, OutputStream out) throws IOException;
}
