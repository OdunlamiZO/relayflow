package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.GuestSessionResponse;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
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

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final HttpSessionSecurityContextRepository securityContextRepository;

    private final MessagingService messagingService;

    private final String sharedBotToken;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository,
            MessagingService messagingService,
            @Value("${shared.telegram.bot-token:}") String sharedBotToken) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.messagingService = messagingService;
        this.sharedBotToken = sharedBotToken;
    }

    public AuthenticatedUserResponse signup(
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
        user.setProvider(AuthenticationProvider.EMAIL);
        user.setProviderSubject(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        log.info("New user registered: email={}", request.email());

        establishSession(request.email(), request.password(), httpRequest, httpResponse);

        return new AuthenticatedUserResponse(
                true, false, user.getId(), user.getEmail(), user.getDisplayName(), null);
    }

    public AuthenticatedUserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        log.info("Login attempt: email={}", request.email());

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
    public GuestSessionResponse createGuestSession(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String anonymousId = UUID.randomUUID().toString();
        String email = "guest-" + anonymousId + "@relayflow.io";

        User user = new User();
        user.setEmail(email);
        user.setProvider(AuthenticationProvider.ANONYMOUS);
        user.setProviderSubject(anonymousId);
        user.setAnonymous(true);
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

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);

        return value == null ? null : value.toString();
    }
}
