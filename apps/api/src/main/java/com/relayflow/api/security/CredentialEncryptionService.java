package com.relayflow.api.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Stores values as ENC:{iv}:{ciphertext}; startup fails without relayflow.encryption.key. */
@Service
public class CredentialEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionService.class);

    private static final String PREFIX = "ENC:";

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;

    public CredentialEncryptionService(@Value("${relayflow.encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "relayflow.encryption.key is required but was not set. "
                            + "Set RELAYFLOW_ENCRYPTION_KEY (generate with: openssl rand -base64 32)");
        }

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("Credential encryption enabled (AES-256-GCM)");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to encrypt credential", exception);
        }
    }

    /** Returns values without the ENC: prefix unchanged, as legacy plaintext. */
    public String decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            return value;
        }

        try {
            String payload = value.substring(PREFIX.length());
            int sep = payload.indexOf(':');
            byte[] iv = Base64.getDecoder().decode(payload.substring(0, sep));
            byte[] ciphertext = Base64.getDecoder().decode(payload.substring(sep + 1));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to decrypt credential", exception);
        }
    }
}
