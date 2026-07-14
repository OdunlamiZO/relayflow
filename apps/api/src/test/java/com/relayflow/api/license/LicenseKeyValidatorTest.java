package com.relayflow.api.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LicenseKeyValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsAValidLicenseKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String publicKeyBase64 =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String licenseKey =
                sign(
                        keyPair.getPrivate(),
                        "acme",
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(3600));

        LicenseKeyValidator validator =
                new LicenseKeyValidator(licenseKey, publicKeyBase64, objectMapper);

        assertThat(validator.claims().customerId()).isEqualTo("acme");
    }

    @Test
    void rejectsATamperedSignature() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String publicKeyBase64 =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String licenseKey =
                sign(
                        otherKeyPair.getPrivate(),
                        "acme",
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> new LicenseKeyValidator(licenseKey, publicKeyBase64, objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("signature verification failed");
    }

    @Test
    void rejectsAMissingLicenseKey() {
        assertThatThrownBy(() -> new LicenseKeyValidator("", "irrelevant", objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("RELAYFLOW_LICENSE_KEY is required");
    }

    @Test
    void rejectsAMissingPublicKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String licenseKey =
                sign(
                        keyPair.getPrivate(),
                        "acme",
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> new LicenseKeyValidator(licenseKey, "", objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("built without a signing public key baked in");
    }

    @Test
    void toleratesAnExpiredKeyWithinTheGracePeriod() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String publicKeyBase64 =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String licenseKey =
                sign(
                        keyPair.getPrivate(),
                        "acme",
                        Instant.now().minusSeconds(7200),
                        Instant.now().minusSeconds(3600));

        LicenseKeyValidator validator =
                new LicenseKeyValidator(licenseKey, publicKeyBase64, objectMapper);

        assertThat(validator.claims().customerId()).isEqualTo("acme");
    }

    @Test
    void rejectsAnExpiredKeyPastTheGracePeriod() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String publicKeyBase64 =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        Instant expiresAt = Instant.now().minusSeconds(20L * 24 * 60 * 60);
        String licenseKey =
                sign(keyPair.getPrivate(), "acme", expiresAt.minusSeconds(3600), expiresAt);

        assertThatThrownBy(() -> new LicenseKeyValidator(licenseKey, publicKeyBase64, objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("grace period has passed");
    }

    private String sign(
            PrivateKey privateKey, String customerId, Instant issuedAt, Instant expiresAt)
            throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "EdDSA");
        String headerB64 = encoder.encodeToString(objectMapper.writeValueAsBytes(header));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", customerId);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        String payloadB64 = encoder.encodeToString(objectMapper.writeValueAsBytes(payload));

        String signingInput = headerB64 + "." + payloadB64;
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String signatureB64 = encoder.encodeToString(signer.sign());

        return signingInput + "." + signatureB64;
    }
}
