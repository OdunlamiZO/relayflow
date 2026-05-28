package com.relayflow.api.configuration;

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

/**
 * AES-256-GCM encryption for sensitive credentials stored in the database (e.g. bot tokens).
 *
 * <p>Encrypted values are stored as {@code ENC:{iv_base64}:{ciphertext_base64}}. If no encryption
 * key is configured the service passes values through unchanged — this is backward compatible and
 * allows the key to be introduced without a data migration.
 *
 * <p>Legacy plaintext values (no {@code ENC:} prefix) are returned as-is from {@link #decrypt},
 * which supports a rolling migration: any plaintext token that was stored before the key was
 * configured will continue to work until it is re-saved.
 *
 * <p>Generate a suitable key with: {@code openssl rand -base64 32}
 */
@Service
public class CredentialEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionService.class);

    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;

    private final boolean enabled;

    public CredentialEncryptionService(@Value("${relayflow.encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn(
                    "relayflow.encryption.key is not set"
                            + " — credentials will be stored as plaintext. "
                            + "Set RELAYFLOW_ENCRYPTION_KEY (generate with: openssl rand -base64 32)");
            this.secretKey = null;
            this.enabled = false;
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            this.enabled = true;
            log.info("Credential encryption enabled (AES-256-GCM)");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Encrypts {@code plaintext}. Returns {@code plaintext} unchanged if encryption is disabled.
     */
    public String encrypt(String plaintext) {
        if (!enabled || plaintext == null) {
            return plaintext;
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a value previously produced by {@link #encrypt}. Returns {@code value} unchanged if:
     *
     * <ul>
     *   <li>encryption is disabled
     *   <li>{@code value} is {@code null}
     *   <li>{@code value} lacks the {@code ENC:} prefix (legacy plaintext — backward compatible)
     * </ul>
     */
    public String decrypt(String value) {
        if (!enabled || value == null || !value.startsWith(PREFIX)) {
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credential", e);
        }
    }
}
