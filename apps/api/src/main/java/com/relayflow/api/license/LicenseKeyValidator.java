package com.relayflow.api.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.stereotype.Service;

/**
 * Verifies the self-hosted license key ({@code RELAYFLOW_LICENSE_KEY}) at startup, entirely
 * offline, against an Ed25519 public key baked into the image at build time.
 *
 * <p>Only active when {@code META-INF/selfhosted.marker} is present on the classpath — baked into
 * the JAR only by {@code mvn package -Pselfhosted} (see {@code pom.xml}, {@code Dockerfile}), never
 * by local dev. Deliberately build-time, not an env-var {@code @ConditionalOnProperty}: env vars
 * always win over {@code application.properties}, so a runtime flag here would be trivially
 * disabled by any self-hosted operator. Disabling this requires modifying the compiled JAR.
 *
 * <p>The verification public key is baked in the same way, at {@code
 * META-INF/license-public-key.txt}, and the grace period is a fixed constant — neither is
 * customer-configurable.
 *
 * <p>The license key is a compact JWS: {@code base64url(header).base64url(payload).base64url(sig)},
 * signed with {@code EdDSA}. The payload carries {@link LicenseClaims}. Verification never makes a
 * network call — self-hosted buyers rely on this working without a dependency on RelayFlow's
 * servers being reachable.
 */
@Service
@ConditionalOnResource(resources = "classpath:META-INF/selfhosted.marker")
public class LicenseKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(LicenseKeyValidator.class);

    private static final long GRACE_PERIOD_DAYS = 14;

    private static final String PUBLIC_KEY_RESOURCE = "META-INF/license-public-key.txt";

    private final LicenseClaims claims;

    public LicenseKeyValidator(
            @Value("${relayflow.license.key:}") String licenseKey,
            @Value("#{T(com.relayflow.api.license.LicenseKeyValidator).loadBakedPublicKey()}")
                    String publicKeyBase64,
            ObjectMapper objectMapper) {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new LicenseVerificationException(
                    "RELAYFLOW_LICENSE_KEY is required for a self-hosted deployment but was not"
                            + " set. See docs/self-hosting.md for how to obtain a license key.");
        }

        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new LicenseVerificationException(
                    "This self-hosted image was built without a signing public key baked in — this"
                            + " is a build configuration error, not something fixable via .env.");
        }

        this.claims = verify(licenseKey, publicKeyBase64, objectMapper);

        Instant now = Instant.now();
        if (now.isAfter(claims.expiresAt())) {
            Instant graceDeadline =
                    claims.expiresAt().plusSeconds(GRACE_PERIOD_DAYS * 24 * 60 * 60);
            if (now.isAfter(graceDeadline)) {
                throw new LicenseVerificationException(
                        "The self-hosted license key expired on "
                                + claims.expiresAt()
                                + " and the "
                                + GRACE_PERIOD_DAYS
                                + "-day grace period has passed. Renew the license to continue.");
            }
            log.warn(
                    "Self-hosted license key expired on {} — running on a {}-day grace period."
                            + " Renew soon to avoid an outage.",
                    claims.expiresAt(),
                    GRACE_PERIOD_DAYS);
        } else {
            log.info(
                    "Self-hosted license key verified for customer {} (valid until {})",
                    claims.customerId(),
                    claims.expiresAt());
        }
    }

    /** Reads the build-time-baked public key. Package-private so tests can call it directly. */
    static String loadBakedPublicKey() {
        try (InputStream inputStream =
                LicenseKeyValidator.class
                        .getClassLoader()
                        .getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (inputStream == null) {
                return "";
            }

            String value = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();

            return value.startsWith("${") ? "" : value;
        } catch (IOException e) {
            return "";
        }
    }

    public LicenseClaims claims() {
        return claims;
    }

    private static LicenseClaims verify(
            String licenseKey, String publicKeyBase64, ObjectMapper objectMapper) {
        String[] parts = licenseKey.split("\\.");
        if (parts.length != 3) {
            throw new LicenseVerificationException(
                    "RELAYFLOW_LICENSE_KEY is not a valid license key.");
        }

        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] signature = decoder.decode(parts[2]);
            byte[] signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);

            PublicKey publicKey = decodePublicKey(publicKeyBase64);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(signingInput);
            if (!verifier.verify(signature)) {
                throw new LicenseVerificationException(
                        "RELAYFLOW_LICENSE_KEY signature verification failed — the key is invalid or"
                                + " was issued for a different deployment.");
            }

            byte[] payloadBytes = decoder.decode(parts[1]);
            RawClaims raw = objectMapper.readValue(payloadBytes, RawClaims.class);
            if (raw.sub == null || raw.iat == null || raw.exp == null) {
                throw new LicenseVerificationException(
                        "RELAYFLOW_LICENSE_KEY is missing required claims.");
            }

            return new LicenseClaims(
                    raw.sub, Instant.ofEpochSecond(raw.iat), Instant.ofEpochSecond(raw.exp));
        } catch (LicenseVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseVerificationException(
                    "RELAYFLOW_LICENSE_KEY could not be verified.", e);
        }
    }

    private static PublicKey decodePublicKey(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(spec);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawClaims {
        public String sub;
        public Long iat;
        public Long exp;
    }
}
