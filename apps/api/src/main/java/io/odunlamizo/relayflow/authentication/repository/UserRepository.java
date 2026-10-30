package io.odunlamizo.relayflow.authentication.repository;

// Docs: docs/codebase-components.md#userrepository

import io.odunlamizo.relayflow.authentication.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
