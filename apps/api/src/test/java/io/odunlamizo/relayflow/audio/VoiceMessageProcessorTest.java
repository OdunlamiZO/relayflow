package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.domain.TranscriptionStatus;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionRequest;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.workflow.engine.ConversationMessageReceivedEvent;
import io.odunlamizo.relayflow.workflow.engine.ConversationOpenedEvent;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class VoiceMessageProcessorTest {

    private final MessageRepository messageRepository = mock(MessageRepository.class);

    private final ChannelMediaDownloader downloader = mock(ChannelMediaDownloader.class);

    private final AudioProviderResolver audioProviderResolver = mock(AudioProviderResolver.class);

    private final StorageProvider storage = mock(StorageProvider.class);

    private final TranscriptionProvider transcription = mock(TranscriptionProvider.class);

    private final List<Object> publishedEvents = new ArrayList<>();

    private final ApplicationEventPublisher eventPublisher = publishedEvents::add;

    private final Message message = new Message();

    private VoiceMessageProcessor processor;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setProvider(ChannelProvider.TELEGRAM);
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setChannelAccount(channelAccount);
        message.setId(UUID.randomUUID());
        message.setWorkspace(workspace);
        message.setConversation(conversation);
        message.setAudioMimeType("audio/ogg");
        message.setTranscriptionStatus(TranscriptionStatus.PENDING);

        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(downloader.provider()).thenReturn(ChannelProvider.TELEGRAM);
        when(downloader.download(channelAccount, "file-1")).thenReturn(new byte[] {1, 2, 3});
        when(storage.id()).thenReturn("LOCAL_DISK");
        when(storage.store(any())).thenReturn("workspace/key");
        when(transcription.id()).thenReturn("OPENAI_COMPATIBLE");
        when(audioProviderResolver.activeStorageProvider()).thenReturn(Optional.of(storage));
        when(audioProviderResolver.activeTranscriptionProvider())
                .thenReturn(Optional.of(transcription));
        when(audioProviderResolver.transcriptionModel("OPENAI_COMPATIBLE"))
                .thenReturn("whisper-large-v3-turbo");

        processor =
                new VoiceMessageProcessor(
                        messageRepository,
                        List.of(downloader),
                        audioProviderResolver,
                        eventPublisher,
                        new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    @Test
    void aTranscribedVoiceNoteOpensTheConversationLikeATextMessage() {
        when(transcription.transcribe(any())).thenReturn(" I want to borrow 50,000 ");

        processor.process(new InboundAudioReceivedEvent(message.getId(), "file-1", true));

        assertThat(message.getText()).isEqualTo("I want to borrow 50,000");
        assertThat(message.getTranscriptionStatus()).isEqualTo(TranscriptionStatus.TRANSCRIBED);
        assertThat(message.getAudioStorageProvider()).isEqualTo("LOCAL_DISK");
        assertThat(message.getAudioStorageKey()).isEqualTo("workspace/key");
        assertThat(publishedEvents)
                .hasAtLeastOneElementOfType(ConversationOpenedEvent.class)
                .hasAtLeastOneElementOfType(SseBroadcastEvent.class);
        verify(transcription)
                .transcribe(
                        org.mockito.ArgumentMatchers.argThat(
                                (TranscriptionRequest request) ->
                                        "whisper-large-v3-turbo".equals(request.model())
                                                && "audio/ogg".equals(request.mimeType())));
    }

    @Test
    void aVoiceNoteInAnOpenConversationIsAReceivedMessage() {
        when(transcription.transcribe(any())).thenReturn("Yes");

        processor.process(new InboundAudioReceivedEvent(message.getId(), "file-1", false));

        assertThat(publishedEvents)
                .hasAtLeastOneElementOfType(ConversationMessageReceivedEvent.class)
                .doesNotHaveAnyElementsOfTypes(ConversationOpenedEvent.class);
    }

    @Test
    void withoutATranscriptionPluginTheVoiceNoteWaitsForAHuman() {
        when(audioProviderResolver.activeTranscriptionProvider()).thenReturn(Optional.empty());

        processor.process(new InboundAudioReceivedEvent(message.getId(), "file-1", true));

        assertThat(message.getText()).isNull();
        assertThat(message.getTranscriptionStatus()).isEqualTo(TranscriptionStatus.NO_PROVIDER);
        assertThat(message.getAudioStorageKey()).isEqualTo("workspace/key");
        assertThat(publishedEvents)
                .doesNotHaveAnyElementsOfTypes(
                        ConversationOpenedEvent.class, ConversationMessageReceivedEvent.class)
                .hasAtLeastOneElementOfType(SseBroadcastEvent.class);
    }

    @Test
    void aFailedDownloadMarksTheTranscriptionFailed() {
        when(downloader.download(any(), any())).thenThrow(new IllegalStateException("gone"));

        processor.process(new InboundAudioReceivedEvent(message.getId(), "file-1", true));

        assertThat(message.getTranscriptionStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(message.getAudioStorageKey()).isNull();
        assertThat(publishedEvents)
                .doesNotHaveAnyElementsOfTypes(
                        ConversationOpenedEvent.class, ConversationMessageReceivedEvent.class);
    }
}
