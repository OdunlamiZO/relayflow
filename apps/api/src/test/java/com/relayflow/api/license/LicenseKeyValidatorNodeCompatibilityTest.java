package com.relayflow.api.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Guards the cross-language contract with {@code apps/marketing/src/lib/license.ts} (the Node
 * signer). Fixtures were generated once by that code — if this test fails, the Node signer has
 * drifted from what this validator expects, and every key the marketing site issues is broken.
 *
 * <p>{@code VALID_KEY}/{@code PAST_GRACE_KEY} use fixed timestamps so this never bit-rots.
 * Grace-period behavior itself is already covered for Java-generated keys by {@link
 * LicenseKeyValidatorTest#toleratesAnExpiredKeyWithinTheGracePeriod()}.
 */
class LicenseKeyValidatorNodeCompatibilityTest {

    private static final String PUBLIC_KEY_BASE64 =
            "MCowBQYDK2VwAyEA/tEyRoi5957TFEoB6t/uqkGydxIdE165/c6S443t+O4=";

    private static final String VALID_KEY =
            "eyJhbGciOiJFZERTQSJ9"
                    + ".eyJzdWIiOiJub2RlLWZpeHR1cmUtY3VzdG9tZXIiLCJpYXQiOjE3NjcyMjU2MDAsImV4cCI6NDA3"
                    + "MDkwODgwMH0"
                    + ".zuZn4lEAbTreYE26pCrBFfmUalEkVI_0KEQK9kzDZnW4ziCo0Yz2gGMYj6rXf8_qSv3fynSRwp_m"
                    + "h_tF9U0qBA";

    private static final String PAST_GRACE_KEY =
            "eyJhbGciOiJFZERTQSJ9"
                    + ".eyJzdWIiOiJub2RlLWZpeHR1cmUtY3VzdG9tZXItZXhwaXJlZCIsImlhdCI6OTE1MTQ4ODAwLCJl"
                    + "eHAiOjk0NjY4NDgwMH0"
                    + ".iOW_wqLE-AKtVOnE0ekhZkyEBjUR3ho2kmCnVO7kjhqAaUtHwWHeEW7PPi1B3Xu1-f6NT2UrxKC1"
                    + "kJYRbWH_CQ";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsANodeSignedKeyAndParsesItsClaims() {
        LicenseKeyValidator validator =
                new LicenseKeyValidator(VALID_KEY, PUBLIC_KEY_BASE64, objectMapper);

        assertThat(validator.claims().customerId()).isEqualTo("node-fixture-customer");
        assertThat(validator.claims().issuedAt()).isEqualTo(Instant.ofEpochSecond(1767225600L));
        assertThat(validator.claims().expiresAt()).isEqualTo(Instant.ofEpochSecond(4070908800L));
    }

    @Test
    void rejectsANodeSignedKeyPastTheGracePeriod() {
        assertThatThrownBy(
                        () ->
                                new LicenseKeyValidator(
                                        PAST_GRACE_KEY, PUBLIC_KEY_BASE64, objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("grace period has passed");
    }

    @Test
    void rejectsANodeSignedKeyWithATamperedSignature() {
        // Flip the first char, not the last — trailing base64url chars can be padding bits a
        // decoder ignores, so tampering them doesn't reliably change the decoded bytes.
        int signatureStart = VALID_KEY.lastIndexOf('.') + 1;
        char firstSignatureChar = VALID_KEY.charAt(signatureStart);
        char differentChar = firstSignatureChar == 'A' ? 'B' : 'A';
        String tamperedKey =
                VALID_KEY.substring(0, signatureStart)
                        + differentChar
                        + VALID_KEY.substring(signatureStart + 1);

        assertThatThrownBy(
                        () -> new LicenseKeyValidator(tamperedKey, PUBLIC_KEY_BASE64, objectMapper))
                .isInstanceOf(LicenseVerificationException.class)
                .hasMessageContaining("signature verification failed");
    }
}
