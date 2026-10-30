package io.odunlamizo.relayflow.authentication;

// Docs: docs/codebase-components.md#emailpassworduserdetailsservice

import io.odunlamizo.relayflow.authentication.domain.AuthenticationProvider;
import io.odunlamizo.relayflow.authentication.domain.UserIdentity;
import io.odunlamizo.relayflow.authentication.repository.UserIdentityRepository;
import io.odunlamizo.relayflow.authentication.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmailPasswordUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    public EmailPasswordUserDetailsService(
            UserRepository userRepository, UserIdentityRepository identityRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        io.odunlamizo.relayflow.authentication.domain.User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("User not found: " + email));

        UserIdentity identity =
                identityRepository
                        .findByUserAndProvider(user, AuthenticationProvider.EMAIL)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "No email credential for: " + email));

        if (identity.getCredential() == null) {
            throw new UsernameNotFoundException("No password credential set for: " + email);
        }

        return User.withUsername(email)
                .password(identity.getCredential())
                .authorities(List.of())
                .build();
    }
}
