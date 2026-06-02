package com.gov.ac.feature.attachment.crypto;

/**
 * SPI for KEK (Key Encryption Key) operations. The default implementation reads a 32-byte AES
 * key from the {@code AC_ATTACHMENT_KEK_HEX} environment variable; HSM / cloud-KMS adapters can
 * replace the bean to keep raw key material outside the JVM.
 *
 * <p>Every method is exception-free at compile time; runtime failures throw {@link
 * CryptoOperationException} so callers can wrap or audit consistently.
 */
public interface KeyProvider {

  /** Identifier of the current KEK (e.g. {@code KEK_V1}). Persisted on every encrypted row. */
  String currentKekRef();

  /** Wraps a freshly generated DEK with the current KEK. */
  WrappedDek wrapDek(byte[] rawDek);

  /** Reverses {@link #wrapDek(byte[])} for the KEK identified by {@code keyRef}. */
  byte[] unwrapDek(byte[] wrappedDek, String keyRef);

  /** Wrapped data-encryption-key + KEK reference returned by {@link #wrapDek(byte[])}. */
  record WrappedDek(byte[] wrapped, String keyRef) {}
}
