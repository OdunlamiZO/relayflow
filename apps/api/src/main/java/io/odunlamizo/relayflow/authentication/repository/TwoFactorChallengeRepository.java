package io.odunlamizo.relayflow.authentication.repository;

import io.odunlamizo.relayflow.authentication.domain.TwoFactorChallenge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, UUID> {

    Optional<TwoFactorChallenge> findByToken(String token);
}
