package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GuestCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(GuestCleanupScheduler.class);

    private final UserRepository userRepository;

    private final GuestCleanupService guestCleanupService;

    private final long guestExpiryHours;

    public GuestCleanupScheduler(
            UserRepository userRepository,
            GuestCleanupService guestCleanupService,
            @Value("${relayflow.guest.expiry-hours}") long guestExpiryHours) {
        this.userRepository = userRepository;
        this.guestCleanupService = guestCleanupService;
        this.guestExpiryHours = guestExpiryHours;
    }

    /**
     * Runs every hour, removes anonymous guest sessions older than {@code
     * relayflow.guest.expiry-hours}.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanupExpiredGuestSessions() {
        Instant cutoff = Instant.now().minus(guestExpiryHours, ChronoUnit.HOURS);
        List<User> expired = userRepository.findExpiredGuests(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} expired guest session(s)", expired.size());

        for (User user : expired) {
            try {
                guestCleanupService.cleanupGuestUser(user);
            } catch (Exception e) {
                log.warn("Failed to clean up guest user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
