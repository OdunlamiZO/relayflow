package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns true when the authenticated principal is an anonymous guest user. */
    public boolean isAnonymous(Authentication authentication) {
        return resolveEmail(authentication)
                .flatMap(userRepository::findByEmail)
                .map(User::isAnonymous)
                .orElse(false);
    }

    /**
     * Resolves the database user ID from the current authentication principal. Handles both
     * email/password (UserDetails) and Google OAuth2 (OAuth2User) principals.
     */
    public UUID resolveUserId(Authentication authentication) {

        return resolveEmail(authentication)
                .flatMap(userRepository::findByEmail)
                .map(user -> user.getId())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Cannot resolve user ID from authentication principal"));
    }

    private Optional<String> resolveEmail(Authentication authentication) {

        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails ud) {
            return Optional.of(ud.getUsername());
        }

        if (principal instanceof OAuth2User oauth) {
            Object email = oauth.getAttributes().get("email");

            return email == null ? Optional.empty() : Optional.of(email.toString());
        }

        return Optional.empty();
    }
}
