package io.odunlamizo.relayflow.authentication.repository;

import io.odunlamizo.relayflow.authentication.domain.UserPreferences;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {}
