package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.UserPreferences;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    // Primary key is user_id, so findById(userId) and deleteById(userId) work directly.
}
