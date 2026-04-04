package com.gov.ac.attachment;

import com.gov.ac.common.api.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalAttachmentContentStore implements AttachmentContentStore {

  private final AttachmentStorageProperties properties;

  @Override
  public void copyToOutputStream(String storageKey, OutputStream out) throws IOException {
    Path file = resolveExistingFile(storageKey);
    try (InputStream in = Files.newInputStream(file)) {
      in.transferTo(out);
    }
  }

  private Path resolveExistingFile(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      throw new NotFoundException("Attachment file not found");
    }
    String key = storageKey.trim().replace('\\', '/');
    if (key.contains("..") || Paths.get(key).isAbsolute()) {
      throw new NotFoundException("Attachment file not found");
    }
    Path root = Paths.get(properties.root()).toAbsolutePath().normalize();
    Path file = root.resolve(key).normalize();
    if (!file.startsWith(root)) {
      throw new NotFoundException("Attachment file not found");
    }
    if (!Files.isRegularFile(file)) {
      throw new NotFoundException("Attachment file not found");
    }
    return file;
  }
}
