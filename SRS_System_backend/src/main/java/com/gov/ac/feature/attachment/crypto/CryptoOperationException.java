package com.gov.ac.feature.attachment.crypto;

/**
 * Wraps any underlying JCE / JCA failure encountered by the attachment crypto layer. Callers
 * should treat it as a server-side fault (5xx); the message intentionally avoids leaking key
 * material or ciphertext details.
 */
public class CryptoOperationException extends RuntimeException {

  public CryptoOperationException(String message) {
    super(message);
  }

  public CryptoOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
