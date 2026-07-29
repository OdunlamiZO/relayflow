package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.PasswordResetToken;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserIdentity;
import com.relayflow.api.authentication.repository.PasswordResetTokenRepository;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.email.EmailService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Issues and consumes single-use password reset links. There is no self-service "change password"
 * form — a user (or, on their behalf, a workspace owner) requests a link, and setting a new
 * password only ever happens by following it.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final String webBaseUrl;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.webBaseUrl = webBaseUrl;
    }

    /**
     * Generates a reset token for the given user and emails it to their address — used both for a
     * user requesting their own reset link and for a workspace owner generating one for a member.
     */
    @Transactional
    public void issueForUser(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found."));

        identityRepository
                .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "This account doesn't use a password — nothing to"
                                                + " reset."));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(userId);
        resetToken = tokenRepository.save(resetToken);

        String resetUrl = webBaseUrl + "/reset-password/" + resetToken.getToken();

        emailService.sendPasswordReset(user.getEmail(), user.getDisplayName(), resetUrl);

        log.info("Password reset link issued: userId={}", userId);
    }

    @Transactional
    public void resetPassword(UUID token, String newPassword) {
        PasswordResetToken resetToken =
                tokenRepository
                        .findByToken(token)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "This reset link is invalid."));

        if (!resetToken.isValid()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This reset link has expired or was already used. Request a new one.");
        }

        User user =
                userRepository
                        .findById(resetToken.getUserId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found."));

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "This account doesn't use a password."));

        identity.setCredential(passwordEncoder.encode(newPassword));
        identityRepository.save(identity);

        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);

        log.info("Password reset via token: userId={}", user.getId());
    }
}
