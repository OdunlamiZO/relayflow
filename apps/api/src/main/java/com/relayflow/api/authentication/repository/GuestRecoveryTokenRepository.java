package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.GuestRecoveryToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRecoveryTokenRepository extends JpaRepository<GuestRecoveryToken, UUID> {

    Optional<GuestRecoveryToken> findByToken(String token);

    @Modifying
    @Query("delete from GuestRecoveryToken t where t.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);
}
