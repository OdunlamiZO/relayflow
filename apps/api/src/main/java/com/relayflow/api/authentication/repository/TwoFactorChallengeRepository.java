package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.TwoFactorChallenge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, UUID> {

    Optional<TwoFactorChallenge> findByToken(String token);
}
