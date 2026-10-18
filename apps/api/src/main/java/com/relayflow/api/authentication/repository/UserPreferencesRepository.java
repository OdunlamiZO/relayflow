package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.UserPreferences;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {}
