package com.relayflow.api.authentication;

import com.relayflow.api.authentication.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmailPasswordUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public EmailPasswordUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.relayflow.api.authentication.domain.User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("User not found: " + email));

        if (user.getPasswordHash() == null) {
            throw new UsernameNotFoundException("No password credential set for user: " + email);
        }

        return User.withUsername(email)
                .password(user.getPasswordHash())
                .authorities(List.of())
                .build();
    }
}
