package io.odunlamizo.relayflow.authentication.repository;

// Docs: docs/codebase-components.md#authentication-satellite-repositories

import io.odunlamizo.relayflow.authentication.domain.UserPreferences;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {}
