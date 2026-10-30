package io.odunlamizo.relayflow.authentication.repository;

// Docs: docs/codebase-components.md#authentication-satellite-repositories

import io.odunlamizo.relayflow.authentication.domain.MfaMethodType;
import io.odunlamizo.relayflow.authentication.domain.User;
import io.odunlamizo.relayflow.authentication.domain.UserMfaMethod;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaMethodRepository extends JpaRepository<UserMfaMethod, UUID> {

    Optional<UserMfaMethod> findByUserAndType(User user, MfaMethodType type);
}
