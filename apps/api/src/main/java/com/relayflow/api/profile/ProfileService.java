package com.relayflow.api.profile;

import com.relayflow.api.authentication.PasswordResetService;
import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.MfaMethodType;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserIdentity;
import com.relayflow.api.authentication.domain.UserMfaMethod;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserMfaMethodRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.profile.dto.OtpRequest;
import com.relayflow.api.profile.dto.ProfileResponse;
import com.relayflow.api.profile.dto.Setup2FAResponse;
import com.relayflow.api.profile.dto.UpdateProfileRequest;
import com.relayflow.api.security.CredentialEncryptionService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    private final UserMfaMethodRepository mfaMethodRepository;

    private final CredentialEncryptionService encryptionService;

    private final TwoFactorService twoFactorService;

    private final PasswordResetService passwordResetService;

    public ProfileService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserMfaMethodRepository mfaMethodRepository,
            CredentialEncryptionService encryptionService,
            TwoFactorService twoFactorService,
            PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.mfaMethodRepository = mfaMethodRepository;
        this.encryptionService = encryptionService;
        this.twoFactorService = twoFactorService;
        this.passwordResetService = passwordResetService;
    }

    public ProfileResponse getProfile(UUID userId) {
        return buildProfile(requireUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setDisplayName(request.displayName());
        user = userRepository.save(user);

        return buildProfile(user);
    }

    public void requestPasswordReset(UUID userId) {
        passwordResetService.issueForUser(userId);
    }

    // ── 2FA ──────────────────────────────────────────────────────────────────

    /**
     * Generates a new TOTP secret, stores it encrypted on the method row (without enabling 2FA
     * yet), and returns the {@code otpauth://} URI for QR code display.
     *
     * <p>The user must call {@link #enable2FA} with a valid OTP to actually turn 2FA on.
     */
    @Transactional
    public Setup2FAResponse setup2FA(UUID userId) {
        User user = requireUser(userId);

        String rawSecret = twoFactorService.generateSecret();

        UserMfaMethod method =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .orElseGet(
                                () -> {
                                    UserMfaMethod m = new UserMfaMethod();
                                    m.setUser(user);
                                    m.setType(MfaMethodType.TOTP);

                                    return m;
                                });

        method.setCredential(encryptionService.encrypt(rawSecret));
        method.setEnabled(false);
        mfaMethodRepository.save(method);

        String uri = twoFactorService.buildOtpauthUri(user.getEmail(), rawSecret);

        return new Setup2FAResponse(uri);
    }

    /** Verifies the OTP against the pending secret and, if valid, enables 2FA. */
    @Transactional
    public void enable2FA(UUID userId, OtpRequest request) {
        User user = requireUser(userId);

        UserMfaMethod method =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "No 2FA setup in progress. Please initiate setup"
                                                        + " first."));

        String rawSecret = encryptionService.decrypt(method.getCredential());

        if (twoFactorService.isInvalidCode(rawSecret, request.otp())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid authenticator code. Please try again.");
        }

        method.setEnabled(true);
        mfaMethodRepository.save(method);

        log.info("2FA enabled: userId={}", userId);
    }

    /** Verifies the current OTP and, if valid, disables and removes the TOTP method. */
    @Transactional
    public void disable2FA(UUID userId, OtpRequest request) {
        User user = requireUser(userId);

        UserMfaMethod method =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Two-factor authentication is not enabled."));

        if (!method.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Two-factor authentication is not enabled.");
        }

        String rawSecret = encryptionService.decrypt(method.getCredential());

        if (twoFactorService.isInvalidCode(rawSecret, request.otp())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid authenticator code. Please try again.");
        }

        mfaMethodRepository.delete(method);

        log.info("2FA disabled: userId={}", userId);
    }

    private User requireUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private ProfileResponse buildProfile(User user) {
        boolean twoFactorEnabled =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .map(UserMfaMethod::isEnabled)
                        .orElse(false);

        List<String> providers =
                identityRepository.findAllByUser(user).stream()
                        .map(UserIdentity::getProvider)
                        .filter(authProvider -> authProvider != AuthenticationProvider.ANONYMOUS)
                        .map(AuthenticationProvider::name)
                        .toList();

        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                twoFactorEnabled,
                providers);
    }
}
