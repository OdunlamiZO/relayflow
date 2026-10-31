package io.odunlamizo.relayflow.contact;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.contact.dto.CreateExternalIdentityRequest;
import io.odunlamizo.relayflow.support.ApplicationIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContactLifecycleIT extends ApplicationIntegrationTest {

    @Autowired private ContactService contacts;

    @Test
    void mergeReassignsIdentityAndConversationAndHidesSource() {
        UUID workspaceId = workspace();
        UUID target = contact(workspaceId);
        UUID source = contact(workspaceId);
        UUID channelId = channel(workspaceId);
        UUID conversationId = conversation(workspaceId, source, channelId);
        var identity =
                contacts.createExternalIdentity(
                        new CreateExternalIdentityRequest(
                                workspaceId,
                                source,
                                channelId,
                                ChannelProvider.TELEGRAM,
                                "external-user",
                                null,
                                "username",
                                Map.of()));
        flushAndClear();

        contacts.mergeContacts(target, source, workspaceId);
        flushAndClear();

        assertThat(contacts.getContactDetail(target, workspaceId).identities())
                .extracting(item -> item.id())
                .containsExactly(identity.id());
        assertThat(
                        jdbc.queryForObject(
                                "select contact_id from conversations where id = ?",
                                UUID.class,
                                conversationId))
                .isEqualTo(target);
        assertThat(
                        jdbc.queryForObject(
                                "select contact_id from external_identities where id = ?",
                                UUID.class,
                                identity.id()))
                .isEqualTo(target);
        assertThatThrownBy(() -> contacts.getContactDetail(source, workspaceId))
                .isInstanceOf(ContactNotFoundException.class);
    }

    @Test
    void deletingContactSoftDeletesConversationMessagesAndIdentity() {
        UUID workspaceId = workspace();
        UUID contactId = contact(workspaceId);
        UUID channelId = channel(workspaceId);
        UUID conversationId = conversation(workspaceId, contactId, channelId);
        UUID messageId = message(workspaceId, conversationId, "Hello", Instant.now());
        var identity =
                contacts.createExternalIdentity(
                        new CreateExternalIdentityRequest(
                                workspaceId,
                                contactId,
                                channelId,
                                ChannelProvider.TELEGRAM,
                                "external-user",
                                null,
                                null,
                                Map.of()));
        flushAndClear();

        contacts.deleteContact(contactId, workspaceId);
        flushAndClear();

        assertThat(deleted("contacts", contactId)).isTrue();
        assertThat(deleted("conversations", conversationId)).isTrue();
        assertThat(deleted("messages", messageId)).isTrue();
        assertThat(deleted("external_identities", identity.id())).isTrue();
        assertThat(contacts.listContacts(workspaceId, 0, 30).items()).isEmpty();
    }

    @Test
    void mergeRejectsTwoUsersOnSameChannelWithoutReassigningThem() {
        UUID workspaceId = workspace();
        UUID target = contact(workspaceId);
        UUID source = contact(workspaceId);
        UUID channelId = channel(workspaceId);
        for (UUID contactId : java.util.List.of(target, source)) {
            contacts.createExternalIdentity(
                    new CreateExternalIdentityRequest(
                            workspaceId,
                            contactId,
                            channelId,
                            ChannelProvider.TELEGRAM,
                            contactId.toString(),
                            null,
                            null,
                            Map.of()));
        }
        flushAndClear();

        assertThatThrownBy(() -> contacts.mergeContacts(target, source, workspaceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same channel account");
        entityManager.clear();
        assertThat(contacts.getContactDetail(source, workspaceId).identities()).hasSize(1);
        assertThat(contacts.getContactDetail(target, workspaceId).identities()).hasSize(1);
    }

    private Boolean deleted(String table, UUID id) {
        return jdbc.queryForObject(
                "select deleted_at is not null from " + table + " where id = ?", Boolean.class, id);
    }
}
