package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.support.ApplicationIntegrationTest;
import io.odunlamizo.relayflow.workflow.engine.ConversationOpenedEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

@RecordApplicationEvents
class VoiceMessageIT extends ApplicationIntegrationTest {

    @Autowired private MockMvc mvc;

    @Autowired private PasswordEncoder encoder;

    @Autowired private ApplicationEvents events;

    @MockBean private AudioProviderResolver audioProviderResolver;

    @Test
    void aTelegramVoiceNoteIsSavedAsPendingAndQueuedForProcessing() throws Exception {
        UUID workspaceId = workspace();
        UUID channelId = channel(workspaceId);

        mvc.perform(
                        post("/telegram/webhook/" + channelId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"update_id":1,"message":{"message_id":7,
                                         "from":{"id":42,"first_name":"Mary"},"chat":{"id":42},
                                         "voice":{"file_id":"voice-file","duration":3,"mime_type":"audio/ogg"}}}
                                        """))
                .andExpect(status().isOk());
        flushAndClear();

        Map<String, Object> message =
                jdbc.queryForMap(
                        "select id, text, audio_mime_type, audio_duration_seconds,"
                                + " transcription_status from messages where workspace_id = ?",
                        workspaceId);
        assertThat(message.get("text")).isNull();
        assertThat(message.get("audio_mime_type")).isEqualTo("audio/ogg");
        assertThat(message.get("audio_duration_seconds")).isEqualTo(3);
        assertThat(message.get("transcription_status")).isEqualTo("PENDING");
        assertThat(events.stream(InboundAudioReceivedEvent.class))
                .containsExactly(
                        new InboundAudioReceivedEvent(
                                (UUID) message.get("id"), "voice-file", true));
        assertThat(events.stream(ConversationOpenedEvent.class)).isEmpty();
    }

    @Test
    void storedAudioPlaysForWorkspaceMembersOnly() throws Exception {
        UUID workspaceId = workspace();
        UUID conversationId = conversation(workspaceId, contact(workspaceId), channel(workspaceId));
        UUID messageId = UUID.randomUUID();
        jdbc.update(
                "insert into messages (id, workspace_id, conversation_id, direction, sender_type,"
                        + " audio_mime_type, audio_storage_provider, audio_storage_key,"
                        + " transcription_status) values (?, ?, ?, 'INBOUND', 'CONTACT',"
                        + " 'audio/ogg', 'FAKE', 'key-1', 'NO_PROVIDER')",
                messageId,
                workspaceId,
                conversationId);
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.load("key-1")).thenReturn("voice".getBytes(StandardCharsets.UTF_8));
        when(audioProviderResolver.storageProvider("FAKE")).thenReturn(Optional.of(storage));

        UUID userId = passwordUser();
        MockHttpSession session = login(userId);
        String path = "/conversations/" + conversationId + "/messages/" + messageId + "/audio";

        mvc.perform(get(path).param("workspaceId", workspaceId.toString()).session(session))
                .andExpect(status().isForbidden());

        jdbc.update(
                "insert into workspace_members (workspace_id, user_id, role) values (?, ?, 'MEMBER')",
                workspaceId,
                userId);

        mvc.perform(get(path).param("workspaceId", workspaceId.toString()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/ogg"))
                .andExpect(content().bytes("voice".getBytes(StandardCharsets.UTF_8)));
    }

    private UUID passwordUser() {
        UUID id = user();
        jdbc.update(
                "insert into user_identities (user_id, provider, provider_subject, credential, verified)"
                        + " values (?, 'EMAIL', ?, ?, true)",
                id,
                id + "@example.com",
                encoder.encode("Voice-password-123!"));

        return id;
    }

    private MockHttpSession login(UUID userId) throws Exception {
        var result =
                mvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\""
                                                        + userId
                                                        + "@example.com\",\"password\":\"Voice-password-123!\"}"))
                        .andExpect(status().isOk())
                        .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
