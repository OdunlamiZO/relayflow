package com.relayflow.api.contact.repository;

import com.relayflow.api.contact.domain.WhitelistedPhoneNumber;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhitelistedPhoneNumberRepository
        extends JpaRepository<WhitelistedPhoneNumber, UUID> {

    @Query(
            "select w from WhitelistedPhoneNumber w where w.workspaceId = :workspaceId order by"
                    + " w.createdAt desc")
    List<WhitelistedPhoneNumber> findByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query(
            "select w from WhitelistedPhoneNumber w where w.id = :id and w.workspaceId ="
                    + " :workspaceId")
    Optional<WhitelistedPhoneNumber> findInWorkspace(
            @Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Query(
            "select w.phoneNumber from WhitelistedPhoneNumber w where w.workspaceId = :workspaceId"
                    + " and w.phoneNumber in :phoneNumbers")
    List<String> findExisting(
            @Param("workspaceId") UUID workspaceId,
            @Param("phoneNumbers") Collection<String> phoneNumbers);

    @Query(
            "select count(w) > 0 from WhitelistedPhoneNumber w where w.workspaceId = :workspaceId"
                    + " and w.phoneNumber = :phoneNumber")
    boolean existsByWorkspaceAndPhoneNumber(
            @Param("workspaceId") UUID workspaceId, @Param("phoneNumber") String phoneNumber);
}
