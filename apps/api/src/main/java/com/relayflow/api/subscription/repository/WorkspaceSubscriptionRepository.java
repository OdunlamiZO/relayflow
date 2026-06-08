package com.relayflow.api.subscription.repository;

import com.relayflow.api.subscription.domain.SubscriptionStatus;
import com.relayflow.api.subscription.domain.WorkspaceSubscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WorkspaceSubscriptionRepository
        extends JpaRepository<WorkspaceSubscription, UUID> {

    Optional<WorkspaceSubscription> findByWorkspaceId(UUID workspaceId);

    Optional<WorkspaceSubscription> findByPaymentSubscriptionCode(String paymentSubscriptionCode);

    Optional<WorkspaceSubscription> findByPaymentCustomerCode(String paymentCustomerCode);

    /**
     * Returns subscriptions that are due for downgrade, with workspace eagerly fetched to avoid
     * lazy-loading issues in the scheduler:
     *
     * <ul>
     *   <li>{@code CANCELLATION_SCHEDULED} where {@code currentPeriodEnd} is in the past.
     *   <li>{@code PAST_DUE} where {@code currentPeriodEnd + 7 days} is in the past (grace period
     *       exhausted).
     * </ul>
     */
    @Transactional(readOnly = true)
    @Query(
            """
            select ws from WorkspaceSubscription ws
            join fetch ws.workspace
            where (ws.status = :cancelStatus and ws.currentPeriodEnd < :now)
               or (ws.status = :pastDueStatus and ws.currentPeriodEnd < :graceCutoff)
            """)
    List<WorkspaceSubscription> findDueForDowngrade(
            @Param("now") Instant now,
            @Param("graceCutoff") Instant graceCutoff,
            @Param("cancelStatus") SubscriptionStatus cancelStatus,
            @Param("pastDueStatus") SubscriptionStatus pastDueStatus);
}
