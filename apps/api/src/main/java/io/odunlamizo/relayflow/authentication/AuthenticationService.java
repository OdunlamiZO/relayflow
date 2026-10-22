package io.odunlamizo.relayflow.authentication;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.authentication.domain.*;
import io.odunlamizo.relayflow.authentication.dto.AuthenticatedUserResponse;
import io.odunlamizo.relayflow.authentication.dto.BootstrapRequest;
import io.odunlamizo.relayflow.authentication.dto.BootstrapResponse;
import io.odunlamizo.relayflow.authentication.dto.InstanceStatusResponse;
import io.odunlamizo.relayflow.authentication.dto.Login2FARequest;
import io.odunlamizo.relayflow.authentication.dto.LoginRequest;
import io.odunlamizo.relayflow.authentication.dto.SignupRequest;
import io.odunlamizo.relayflow.authentication.dto.SignupResponse;
import io.odunlamizo.relayflow.authentication.repository.TwoFactorChallengeRepository;
import io.odunlamizo.relayflow.authentication.repository.UserIdentityRepository;
import io.odunlamizo.relayflow.authentication.repository.UserMfaMethodRepository;
import io.odunlamizo.relayflow.authentication.repository.UserPreferencesRepository;
import io.odunlamizo.relayflow.authentication.repository.UserRepository;
import io.odunlamizo.relayflow.profile.TwoFactorService;
import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import io.odunlamizo.relayflow.workspace.WorkspaceInviteService;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import io.odunlamizo.relayflow.workspace.dto.CreateWorkspaceRequest;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceResponse;
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

    private final WorkspaceService workspaceService;

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
            WorkspaceService workspaceService,
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
        this.workspaceService = workspaceService;
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
        // The invite token vouches for the email, so no verification email is sent.
        identity.setVerified(true);
        identityRepository.save(identity);

        // Throws on a revoked, expired, or mismatched invite, rolling back the new user rows.
        UUID workspaceId = workspaceInviteService.acceptInvite(request.inviteToken(), user.getId());

        establishSessionForUser(user, httpRequest, httpResponse);

        log.info(
                "User registered via invite {} {}",
                kv("workspaceId", workspaceId),
                kv("userId", user.getId()));

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
        // Mail delivery may not be configured yet, so a bootstrapped admin skips verification.
        identity.setVerified(true);
        identityRepository.save(identity);

        WorkspaceResponse workspace =
                workspaceService.createWorkspace(
                        new CreateWorkspaceRequest(request.workspaceName()), user.getId());

        establishSessionForUser(user, httpRequest, httpResponse);

        log.info(
                "Instance bootstrapped {} {}",
                kv("workspaceId", workspace.id()),
                kv("userId", user.getId()));

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
        log.debug("Login attempt {}", kv("email", request.email()));

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Invalid email or password."));

        // Google-only accounts get the same generic error as a wrong password.
        identityRepository
                .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        establishSession(request.email(), request.password(), httpRequest, httpResponse);

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

            log.info("Two-factor challenge issued {}", kv("userId", user.getId()));

            return new AuthenticatedUserResponse(
                    false, null, null, null, null, true, challengeToken);
        }

        log.info("User logged in {} {}", kv("userId", user.getId()), kv("method", "password"));

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

        log.info("User logged in {} {}", kv("userId", user.getId()), kv("method", "password_2fa"));

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

    private void establishSessionForUser(
            User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password("")
                        .authorities(List.of())
                        .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

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
