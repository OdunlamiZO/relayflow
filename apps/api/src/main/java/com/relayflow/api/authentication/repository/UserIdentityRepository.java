package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderSubject(
            AuthenticationProvider provider, String providerSubject);

    Optional<UserIdentity> findByUserAndProvider(User user, AuthenticationProvider provider);

    List<UserIdentity> findAllByUser(User user);
}
