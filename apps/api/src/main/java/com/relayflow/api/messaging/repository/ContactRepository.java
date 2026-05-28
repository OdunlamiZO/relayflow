package com.relayflow.api.messaging.repository;

import com.relayflow.api.messaging.domain.Contact;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    void deleteByWorkspaceId(UUID workspaceId);
}
