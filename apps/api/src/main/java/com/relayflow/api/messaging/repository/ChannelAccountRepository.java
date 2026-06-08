package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelAccountRepository extends JpaRepository<ChannelAccount, UUID> {

    @Modifying
    @Query("UPDATE ChannelAccount ca SET ca.deletedAt = :now WHERE ca.workspace.id = :workspaceId")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Query("select ca from ChannelAccount ca where ca.workspace.id = :workspaceId")
    List<ChannelAccount> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("select ca from ChannelAccount ca where ca.id = :id and ca.workspace.id = :workspaceId")
    Optional<ChannelAccount> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            "select ca from ChannelAccount ca where ca.workspace.id = :workspaceId and ca.provider = :provider")
    List<ChannelAccount> findAllByProvider(
            @Param("workspaceId") UUID workspaceId, @Param("provider") ChannelProvider provider);

    /**
     * Counts non-deleted, non-shared channel accounts in a workspace.
     *
     * <p>Pass {@code null} for {@code status} to count all statuses (used when creating — total
     * count regardless of enabled/disabled). Pass {@link
     * com.relayflow.api.messaging.domain.ChannelAccountStatus#ACTIVE} to count only active ones
     * (used when re-enabling — to enforce the live-account limit).
     *
     * <p>Shared (guest-bot) accounts are created by the system and never count toward the plan
     * limit.
     */
    @Query(
            "select count(ca) from ChannelAccount ca where ca.workspace.id = :workspaceId and ca.shared = false and (:status is null or ca.status = :status)")
    long countBillable(
            @Param("workspaceId") UUID workspaceId, @Param("status") ChannelAccountStatus status);

    /**
     * Returns all non-shared channel accounts for the workspace in the requested order. Spring Data
     * appends the {@code ORDER BY} clause from the {@code sort} argument at runtime.
     *
     * <p>Example — newest first: {@code findBillable(workspaceId, Sort.by(Direction.DESC,
     * "createdAt"))}
     */
    @Query(
            "select ca from ChannelAccount ca where ca.workspace.id = :workspaceId and ca.shared = false")
    List<ChannelAccount> findBillable(@Param("workspaceId") UUID workspaceId, Sort sort);
}
