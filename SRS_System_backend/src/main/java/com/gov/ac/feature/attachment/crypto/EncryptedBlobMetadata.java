package com.gov.ac.feature.attachment.crypto;

/** Cryptographic metadata for a freshly-encrypted attachment blob. */
public record EncryptedBlobMetadata(
    String algorithm,
    String keyRef,
    byte[] wrappedDek,
    byte[] iv,
    String plaintextSha256,
    String ciphertextSha256,
    long plaintextByteSize,
    long ciphertextByteSize) {}
