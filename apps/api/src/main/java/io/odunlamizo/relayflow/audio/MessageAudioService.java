package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#messageaudiocontroller-and-messageaudioservice

import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageAudioService {

    private final MessageRepository messageRepository;

    private final AudioProviderResolver audioProviderResolver;

    public MessageAudioService(
            MessageRepository messageRepository, AudioProviderResolver audioProviderResolver) {
        this.messageRepository = messageRepository;
        this.audioProviderResolver = audioProviderResolver;
    }

    @Transactional(readOnly = true)
    public AudioFile getAudio(UUID workspaceId, UUID conversationId, UUID messageId) {
        Message message =
                messageRepository
                        .findById(messageId)
                        .filter(found -> found.getWorkspace().getId().equals(workspaceId))
                        .filter(found -> found.getConversation().getId().equals(conversationId))
                        .filter(found -> found.getAudioStorageKey() != null)
                        .orElseThrow(() -> new ResourceNotFoundException("Audio not found"));

        StorageProvider storage =
                audioProviderResolver
                        .storageProvider(message.getAudioStorageProvider())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "The storage plugin "
                                                        + message.getAudioStorageProvider()
                                                        + " that holds this audio isn't installed"));

        return new AudioFile(
                storage.load(message.getAudioStorageKey()), message.getAudioMimeType());
    }

    public record AudioFile(byte[] content, String mimeType) {}
}
