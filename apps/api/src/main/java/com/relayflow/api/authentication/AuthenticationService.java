package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.*;
import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.BootstrapRequest;
import com.relayflow.api.authentication.dto.BootstrapResponse;
import com.relayflow.api.authentication.dto.InstanceStatusResponse;
import com.relayflow.api.authentication.dto.Login2FARequest;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.dto.SignupResponse;
import com.relayflow.api.authentication.repository.TwoFactorChallengeRepository;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserMfaMethodRepository;
import com.relayflow.api.authentication.repository.UserPreferencesRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.WorkspaceInviteService;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import com.relayflow.api.profile.TwoFactorService;
import com.relayflow.api.security.CredentialEncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final TwoFactorChallengeRepository challengeRepository;

    private final WorkspaceInviteService workspaceInviteService;

    private final MessagingService messagingService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final HttpSessionSecurityContextRepository securityContextRepository;

    private final TwoFactorService twoFactorService;

    private final CredentialEncryptionService encryptionService;

    public AuthenticationService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserPreferencesRepository prefsRepository,
            UserMfaMethodRepository mfaMethodRepository,
            TwoFactorChallengeRepository challengeRepository,
            WorkspaceInviteService workspaceInviteService,
            MessagingService messagingService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository,
            TwoFactorService twoFactorService,
            CredentialEncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.prefsRepository = prefsRepository;
        this.mfaMethodRepository = mfaMethodRepository;
        this.challengeRepository = challengeRepository;
        this.workspaceInviteService = workspaceInviteService;
        this.messagingService = messagingService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.twoFactorService = twoFactorService;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public SignupResponse signup(
            SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.name());
        user = userRepository.save(user);

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefsRepository.save(prefs);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthenticationProvider.EMAIL);
        identity.setProviderSubject(request.email());
        identity.setCredential(passwordEncoder.encode(request.password()));
        // The invite itself is the vouch — same trust level acceptInvite() already applies
        // (case-insensitive email match, no re-verification), so no email round-trip here.
        identity.setVerified(true);
        identityRepository.save(identity);

        // Reuses the invite's existing status handling (410 on REVOKED/EXPIRED) and
        // case-insensitive email-match check (403) verbatim — a bad/expired/mismatched
        // token throws from inside this call, and @Transactional rolls back the user
        // rows created above.
        UUID workspaceId = workspaceInviteService.acceptInvite(request.inviteToken(), user.getId());

        establishSessionForUser(user, httpRequest, httpResponse);

        log.info(
                "New user registered via invite: email={}, workspace={}",
                request.email(),
                workspaceId);

        return new SignupResponse(
                true, user.getId(), user.getEmail(), user.getDisplayName(), workspaceId);
    }

    @Transactional
    public BootstrapResponse bootstrap(
            BootstrapRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (userRepository.count() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This instance has already been set up");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.name());
        user = userRepository.save(user);

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefsRepository.save(prefs);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthenticationProvider.EMAIL);
        identity.setProviderSubject(request.email());
        identity.setCredential(passwordEncoder.encode(request.password()));
        // The operator has direct server access to provision a fresh instance — there's no
        // guarantee email delivery is even configured yet, so there's nothing to verify against.
        identity.setVerified(true);
        identityRepository.save(identity);

        WorkspaceResponse workspace =
                messagingService.createWorkspace(
                        new CreateWorkspaceRequest(request.workspaceName()), user.getId());

        establishSessionForUser(user, httpRequest, httpResponse);

        log.info("Instance bootstrapped: admin={}, workspace={}", request.email(), workspace.id());

        return new BootstrapResponse(
                true, user.getId(), user.getEmail(), user.getDisplayName(), workspace.id());
    }

    @Transactional(readOnly = true)
    public InstanceStatusResponse getInstanceStatus() {
        return new InstanceStatusResponse(userRepository.count() > 0);
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

        // Confirms the account actually has an EMAIL/password identity (as opposed to
        // Google-OAuth-only), so a mismatched login method gives the same generic error
        // rather than a confusing failure from authenticationManager.authenticate() below.
        identityRepository
                .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Invalid email or password."));

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
                    false, null, null, null, null, true, challengeToken);
        }

        return new AuthenticatedUserResponse(
                true,
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
                        .map(mfaMethod -> encryptionService.decrypt(mfaMethod.getCredential()))
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
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                false,
                null);
    }

    public AuthenticatedUserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthenticatedUserResponse(false, null, null, null, null, false, null);
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
                                            user.getId(),
                                            email,
                                            stringAttribute(attributes, "name"),
                                            stringAttribute(attributes, "picture"),
                                            false,
                                            null))
                    .orElse(
                            new AuthenticatedUserResponse(
                                    true,
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
                                            user.getId(),
                                            user.getEmail(),
                                            user.getDisplayName(),
                                            user.getAvatarUrl(),
                                            false,
                                            null))
                    .orElse(
                            new AuthenticatedUserResponse(
                                    false, null, null, null, null, false, null));
        }

        return new AuthenticatedUserResponse(false, null, null, null, null, false, null);
    }

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
     * Establishes a session without requiring the plaintext password. Used after signup, instance
     * bootstrap, and 2FA completion.
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
