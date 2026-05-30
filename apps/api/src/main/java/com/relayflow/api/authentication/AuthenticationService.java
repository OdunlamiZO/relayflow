package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.EmailVerificationToken;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.GuestSessionResponse;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.dto.SignupResponse;
import com.relayflow.api.authentication.repository.EmailVerificationTokenRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.email.EmailService;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
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

    private final EmailVerificationTokenRepository verificationTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final HttpSessionSecurityContextRepository securityContextRepository;

    private final MessagingService messagingService;

    private final EmailService emailService;

    private final String webBaseUrl;

    private final String sharedBotToken;

    public AuthenticationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository,
            MessagingService messagingService,
            EmailService emailService,
            @Value("${relayflow.web.base-url}") String webBaseUrl,
            @Value("${shared.telegram.bot-token:}") String sharedBotToken) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.messagingService = messagingService;
        this.emailService = emailService;
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
        user.setProvider(AuthenticationProvider.EMAIL);
        user.setProviderSubject(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user = userRepository.save(user);

        String rawToken = generateToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(rawToken);
        verificationToken.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        verificationTokenRepository.save(verificationToken);

        String verifyUrl = webBaseUrl + "/verify-email?token=" + rawToken;
        emailService.sendEmailVerification(request.email(), request.name(), verifyUrl);

        log.info("New user registered: email={}", request.email());

        return new SignupResponse(true);
    }

    public AuthenticatedUserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        log.info("Login attempt: email={}", request.email());

        userRepository
                .findByEmail(request.email())
                .ifPresent(
                        user -> {
                            if (user.getProvider() == AuthenticationProvider.EMAIL
                                    && !user.isEmailVerified()) {
                                throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Please verify your email address before logging in. Check your inbox.");
                            }
                        });

        establishSession(request.email(), request.password(), httpRequest, httpResponse);

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR));

        return new AuthenticatedUserResponse(
                true,
                user.isAnonymous(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl());
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
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.markUsed();
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified: email={}", user.getEmail());

        establishSessionForUser(user, httpRequest, httpResponse);

        return new AuthenticatedUserResponse(
                true, false, user.getId(), user.getEmail(), user.getDisplayName(), null);
    }

    @Transactional
    public GuestSessionResponse createGuestSession(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String anonymousId = UUID.randomUUID().toString();
        String email = "guest-" + anonymousId + "@relayflow.io";

        User user = new User();
        user.setEmail(email);
        user.setProvider(AuthenticationProvider.ANONYMOUS);
        user.setProviderSubject(anonymousId);
        user.setAnonymous(true);
        user.setEmailVerified(true);
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        WorkspaceResponse workspace =
                messagingService.createWorkspace(
                        new CreateWorkspaceRequest("Guest Workspace"), user.getId());

        if (sharedBotToken != null && !sharedBotToken.isBlank()) {
            messagingService.createSharedBotChannelAccount(workspace.id(), sharedBotToken);
        }

        log.info("Guest session created: userId={}, workspaceId={}", user.getId(), workspace.id());

        establishAnonymousSession(user, httpRequest, httpResponse);

        return new GuestSessionResponse(workspace.id());
    }

    public AuthenticatedUserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthenticatedUserResponse(false, false, null, null, null, null);
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
                                            stringAttribute(attributes, "picture")))
                    .orElse(
                            new AuthenticatedUserResponse(
                                    true,
                                    false,
                                    null,
                                    email,
                                    stringAttribute(attributes, "name"),
                                    stringAttribute(attributes, "picture")));
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
                                            user.getAvatarUrl()))
                    .orElse(new AuthenticatedUserResponse(false, false, null, null, null, null));
        }

        return new AuthenticatedUserResponse(false, false, null, null, null, null);
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
     * Establishes a session for a verified email user without requiring the plaintext password.
     * Used after email verification so the user is logged in immediately.
     */
    private void establishSessionForUser(
            User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
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

    private void establishAnonymousSession(
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
