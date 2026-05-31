package com.relayflow.api.profile;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.MfaMethodType;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserIdentity;
import com.relayflow.api.authentication.domain.UserMfaMethod;
import com.relayflow.api.authentication.domain.UserPreferences;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserMfaMethodRepository;
import com.relayflow.api.authentication.repository.UserPreferencesRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.configuration.CredentialEncryptionService;
import com.relayflow.api.profile.dto.ChangePasswordRequest;
import com.relayflow.api.profile.dto.DeleteAccountRequest;
import com.relayflow.api.profile.dto.OtpRequest;
import com.relayflow.api.profile.dto.ProfileResponse;
import com.relayflow.api.profile.dto.Setup2FAResponse;
import com.relayflow.api.profile.dto.UpdateProfileRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    private final UserPreferencesRepository prefsRepository;

    private final UserMfaMethodRepository mfaMethodRepository;

    private final PasswordEncoder passwordEncoder;

    private final CredentialEncryptionService encryptionService;

    private final TwoFactorService twoFactorService;

    public ProfileService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserPreferencesRepository prefsRepository,
            UserMfaMethodRepository mfaMethodRepository,
            PasswordEncoder passwordEncoder,
            CredentialEncryptionService encryptionService,
            TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.prefsRepository = prefsRepository;
        this.mfaMethodRepository = mfaMethodRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.twoFactorService = twoFactorService;
    }

    public ProfileResponse getProfile(UUID userId) {
        return buildProfile(requireUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setDisplayName(request.displayName());
        user = userRepository.save(user);

        UserPreferences prefs = requirePrefs(userId, user);
        prefs.setReceiveEmailUpdates(request.receiveEmailUpdates());
        prefsRepository.save(prefs);

        return buildProfile(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Password changes are only supported for"
                                                        + " email/password accounts."));

        if (!passwordEncoder.matches(request.currentPassword(), identity.getCredential())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        identity.setCredential(passwordEncoder.encode(request.newPassword()));
        identityRepository.save(identity);

        log.info("Password changed: userId={}", userId);
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = requireUser(userId);

        identityRepository
                .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                .ifPresent(
                        identity -> {
                            if (request.password() == null || request.password().isBlank()) {
                                throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Password is required to delete your account.");
                            }

                            if (!passwordEncoder.matches(
                                    request.password(), identity.getCredential())) {
                                throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Incorrect password.");
                            }
                        });

        // Hard-delete satellite rows so the email address can be re-registered later.
        identityRepository.deleteAllByUser(user);
        mfaMethodRepository.deleteAllByUser(user);
        prefsRepository.deleteById(userId);

        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        log.info("Account deleted: userId={}", userId);
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

        if (!twoFactorService.verifyCode(rawSecret, request.otp())) {
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

        if (!twoFactorService.verifyCode(rawSecret, request.otp())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid authenticator code. Please try again.");
        }

        mfaMethodRepository.delete(method);

        log.info("2FA disabled: userId={}", userId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User requireUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private UserPreferences requirePrefs(UUID userId, User user) {
        return prefsRepository
                .findById(userId)
                .orElseGet(
                        () -> {
                            UserPreferences prefs = new UserPreferences();
                            prefs.setUser(user);

                            return prefsRepository.save(prefs);
                        });
    }

    private ProfileResponse buildProfile(User user) {
        boolean twoFactorEnabled =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .map(UserMfaMethod::isEnabled)
                        .orElse(false);

        UserPreferences prefs = requirePrefs(user.getId(), user);

        List<String> providers =
                identityRepository.findAllByUser(user).stream()
                        .map(UserIdentity::getProvider)
                        .filter(p -> p != AuthenticationProvider.ANONYMOUS)
                        .map(AuthenticationProvider::name)
                        .toList();

        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                prefs.isReceiveEmailUpdates(),
                twoFactorEnabled,
                providers);
    }
}
