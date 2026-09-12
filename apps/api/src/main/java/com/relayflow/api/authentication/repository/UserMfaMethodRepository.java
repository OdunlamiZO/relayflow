package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.MfaMethodType;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserMfaMethod;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaMethodRepository extends JpaRepository<UserMfaMethod, UUID> {

    Optional<UserMfaMethod> findByUserAndType(User user, MfaMethodType type);
}
