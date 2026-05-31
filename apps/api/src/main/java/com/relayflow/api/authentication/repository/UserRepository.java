package com.relayflow.api.authentication.repository;

import com.relayflow.api.authentication.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query(
            """
            select u from User u
            where u.anonymous = true
              and u.createdAt < :cutoff
            """)
    List<User> findExpiredGuests(@Param("cutoff") Instant cutoff);
}
