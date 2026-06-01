package com.relayflow.api.profile;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

/**
 * Handles TOTP (Time-based One-Time Password) operations for two-factor authentication.
 *
 * <p>Secrets are generated as base32 strings. The caller is responsible for encrypting them before
 * persistence and decrypting them before verification.
 */
@Service
public class TwoFactorService {

    private static final String ISSUER = "RelayFlow";

    private final DefaultCodeVerifier codeVerifier;

    public TwoFactorService() {
        DefaultCodeVerifier verifier =
                new DefaultCodeVerifier(
                        new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(1);

        this.codeVerifier = verifier;
    }

    /** Generates a new base32-encoded TOTP secret (160 bits). */
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    /**
     * Builds an {@code otpauth://} URI suitable for QR code generation.
     *
     * @param email user's email address (used as the account label)
     * @param base32Secret the raw (unencrypted) base32 TOTP secret
     */
    public String buildOtpauthUri(String email, String base32Secret) {
        String label =
                URLEncoder.encode(ISSUER + ":" + email, StandardCharsets.UTF_8).replace("+", "%20");
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8).replace("+", "%20");

        return "otpauth://totp/"
                + label
                + "?secret="
                + base32Secret
                + "&issuer="
                + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * Returns {@code true} if the OTP does <em>not</em> match the secret. Allows ±1 time period of
     * clock skew. Callers use this as a guard: {@code if (isInvalidCode(...)) throw ...}
     */
    public boolean isInvalidCode(String base32Secret, String otp) {
        return !codeVerifier.isValidCode(base32Secret, otp);
    }
}
