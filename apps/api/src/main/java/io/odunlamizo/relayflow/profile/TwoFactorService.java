package io.odunlamizo.relayflow.profile;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

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

    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

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

    /** Allows one time period of clock skew either way. */
    public boolean isInvalidCode(String base32Secret, String otp) {
        return !codeVerifier.isValidCode(base32Secret, otp);
    }
}
