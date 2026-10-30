package io.odunlamizo.relayflow.authentication.repository;

// Docs: docs/codebase-components.md#authentication-satellite-repositories

import io.odunlamizo.relayflow.authentication.domain.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(UUID token);
}
