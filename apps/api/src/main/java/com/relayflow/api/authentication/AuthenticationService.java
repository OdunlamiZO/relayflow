package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.*;
import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.GuestSessionResponse;
import com.relayflow.api.authentication.dto.Login2FARequest;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.dto.SignupResponse;
import com.relayflow.api.authentication.repository.EmailVerificationTokenRepository;
import com.relayflow.api.authentication.repository.TwoFactorChallengeRepository;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserMfaMethodRepository;
import com.relayflow.api.authentication.repository.UserPreferencesRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.configuration.CredentialEncryptionService;
import com.relayflow.api.email.EmailService;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import com.relayflow.api.profile.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    private final UserPreferencesRepository prefsRepository;

    private final UserMfaMethodRepository mfaMethodRepository;

    private final EmailVerificationTokenRepository verificationTokenRepository;

    private final TwoFactorChallengeRepository challengeRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final HttpSessionSecurityContextRepository securityContextRepository;

    private final MessagingService messagingService;

    private final EmailService emailService;

    private final TwoFactorService twoFactorService;

    private final CredentialEncryptionService encryptionService;

    private final String webBaseUrl;

    private final String sharedBotToken;

    public AuthenticationService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserPreferencesRepository prefsRepository,
            UserMfaMethodRepository mfaMethodRepository,
            EmailVerificationTokenRepository verificationTokenRepository,
            TwoFactorChallengeRepository challengeRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository,
            MessagingService messagingService,
            EmailService emailService,
            TwoFactorService twoFactorService,
            CredentialEncryptionService encryptionService,
            @Value("${relayflow.web.base-url}") String webBaseUrl,
            @Value("${shared.telegram.bot-token:}") String sharedBotToken) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.prefsRepository = prefsRepository;
        this.mfaMethodRepository = mfaMethodRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.messagingService = messagingService;
        this.emailService = emailService;
        this.twoFactorService = twoFactorService;
        this.encryptionService = encryptionService;
        this.webBaseUrl = webBaseUrl;
        this.sharedBotToken = sharedBotToken;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.name());
        user = userRepository.save(user);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthenticationProvider.EMAIL);
        identity.setProviderSubject(request.email());
        identity.setCredential(passwordEncoder.encode(request.password()));
        identity.setVerified(false);
        identityRepository.save(identity);

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefsRepository.save(prefs);

        String rawToken = generateToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(rawToken);
        verificationToken.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        verificationTokenRepository.save(verificationToken);

        String verifyUrl = webBaseUrl + "/verify-email?token=" + rawToken;

        if (request.returnUrl() != null
                && !request.returnUrl().isBlank()
                && request.returnUrl().startsWith("/")) {
            verifyUrl +=
                    "&returnUrl=" + URLEncoder.encode(request.returnUrl(), StandardCharsets.UTF_8);
        }

        emailService.sendEmailVerification(request.email(), request.name(), verifyUrl);

        log.info("New user registered: email={}", request.email());

        return new SignupResponse(true);
    }

    @Transactional
    public AuthenticatedUserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        log.info("Login attempt: email={}", request.email());

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Invalid email or password."));

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Invalid email or password."));

        if (!identity.isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Please verify your email address before logging in. Check your inbox.");
        }

        // Validate credentials — throws BadCredentialsException if wrong.
        establishSession(request.email(), request.password(), httpRequest, httpResponse);

        // If 2FA is enabled, invalidate the session we just created and issue a challenge instead.
        boolean has2FA =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .map(UserMfaMethod::isEnabled)
                        .orElse(false);

        if (has2FA) {
            invalidateSession(httpRequest, httpResponse);

            String challengeToken = generateToken();
            TwoFactorChallenge challenge = new TwoFactorChallenge();
            challenge.setUser(user);
            challenge.setToken(challengeToken);
            challenge.setExpiresAt(Instant.now().plus(Duration.ofMinutes(5)));
            challengeRepository.save(challenge);

            log.info("2FA challenge issued: email={}", request.email());

            return new AuthenticatedUserResponse(
                    false, false, null, null, null, null, true, challengeToken);
        }

        return new AuthenticatedUserResponse(
                true,
                user.isAnonymous(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                false,
                null);
    }

    @Transactional
    public AuthenticatedUserResponse login2FA(
            Login2FARequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        TwoFactorChallenge challenge =
                challengeRepository
                        .findByToken(request.challengeToken())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Invalid or expired 2FA challenge."));

        if (challenge.isExpired()) {
            challengeRepository.delete(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This 2FA challenge has expired. Please log in again.");
        }

        User user = challenge.getUser();

        String rawSecret =
                mfaMethodRepository
                        .findByUserAndType(user, MfaMethodType.TOTP)
                        .map(m -> encryptionService.decrypt(m.getCredential()))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "2FA method not found."));

        if (twoFactorService.isInvalidCode(rawSecret, request.otp())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid authenticator code. Please try again.");
        }

        challengeRepository.delete(challenge);
        establishSessionForUser(user, httpRequest, httpResponse);

        log.info("Login via 2FA: email={}", user.getEmail());

        return new AuthenticatedUserResponse(
                true,
                user.isAnonymous(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                false,
                null);
    }

    @Transactional
    public AuthenticatedUserResponse verifyEmail(
            String rawToken, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        EmailVerificationToken verificationToken =
                verificationTokenRepository
                        .findByToken(rawToken)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Invalid or expired verification link."));

        if (verificationToken.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This verification link has already been used. Please sign in.");
        }

        if (verificationToken.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This verification link has expired. Please sign up again.");
        }

        User user = verificationToken.getUser();

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Identity not found for user."));

        identity.setVerified(true);
        identityRepository.save(identity);

        verificationToken.markUsed();
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified: email={}", user.getEmail());

        establishSessionForUser(user, httpRequest, httpResponse);

        return new AuthenticatedUserResponse(
                true,
                false,
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                null,
                false,
                null);
    }

    @Transactional
    public GuestSessionResponse createGuestSession(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String anonymousId = UUID.randomUUID().toString();
        String email = "guest-" + anonymousId + "@relayflow.io";

        User user = new User();
        user.setEmail(email);
        user.setAnonymous(true);
        user.setLastActiveAt(Instant.now());
        user = userRepository.save(user);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthenticationProvider.ANONYMOUS);
        identity.setProviderSubject(anonymousId);
        identity.setVerified(true);
        identityRepository.save(identity);

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefsRepository.save(prefs);

        WorkspaceResponse workspace =
                messagingService.createWorkspace(
                        new CreateWorkspaceRequest("Guest Workspace"), user.getId());

        if (sharedBotToken != null && !sharedBotToken.isBlank()) {
            messagingService.createSharedBotChannelAccount(workspace.id(), sharedBotToken);
        }

        log.info("Guest session created: userId={}, workspaceId={}", user.getId(), workspace.id());

        establishSessionForUser(user, httpRequest, httpResponse);

        return new GuestSessionResponse(workspace.id());
    }

    public AuthenticatedUserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthenticatedUserResponse(false, false, null, null, null, null, false, null);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauthUser) {
            Map<String, Object> attributes = oauthUser.getAttributes();
            String email = stringAttribute(attributes, "email");

            return userRepository
                    .findByEmail(email)
                    .map(
                            user ->
                                    new AuthenticatedUserResponse(
                                            true,
                                            false,
                                            user.getId(),
                                            email,
                                            stringAttribute(attributes, "name"),
                                            stringAttribute(attributes, "picture"),
                                            false,
                                            null))
                    .orElse(
                            new AuthenticatedUserResponse(
                                    true,
                                    false,
                                    null,
                                    email,
                                    stringAttribute(attributes, "name"),
                                    stringAttribute(attributes, "picture"),
                                    false,
                                    null));
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository
                    .findByEmail(userDetails.getUsername())
                    .map(
                            user ->
                                    new AuthenticatedUserResponse(
                                            true,
                                            user.isAnonymous(),
                                            user.getId(),
                                            user.isAnonymous() ? null : user.getEmail(),
                                            user.getDisplayName(),
                                            user.getAvatarUrl(),
                                            false,
                                            null))
                    .orElse(
                            new AuthenticatedUserResponse(
                                    false, false, null, null, null, null, false, null));
        }

        return new AuthenticatedUserResponse(false, false, null, null, null, null, false, null);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private void establishSession(
            String email,
            String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, password));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Establishes a session without requiring the plaintext password. Used after email
     * verification, 2FA completion, and anonymous session creation.
     */
    private void establishSessionForUser(
            User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password("")
                        .authorities(List.of())
                        .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        SecurityContextHolder.setContext(context);
    }

    private void invalidateSession(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);

        return value == null ? null : value.toString();
    }
}
