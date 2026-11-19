package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#voicemessageprocessor

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.domain.TranscriptionStatus;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.plugin.StorageProvider;
import io.odunlamizo.relayflow.plugin.StorageRequest;
import io.odunlamizo.relayflow.plugin.TranscriptionProvider;
import io.odunlamizo.relayflow.plugin.TranscriptionRequest;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.workflow.engine.ConversationMessageReceivedEvent;
import io.odunlamizo.relayflow.workflow.engine.ConversationOpenedEvent;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** Downloads, stores, and transcribes inbound voice notes, then hands transcripts on. */
@Component
public class VoiceMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(VoiceMessageProcessor.class);

    /** The upload limit of OpenAI's transcription API. */
    static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024;

    private final MessageRepository messageRepository;

    private final Map<ChannelProvider, ChannelMediaDownloader> downloaders;

    private final AudioProviderResolver audioProviderResolver;

    private final ApplicationEventPublisher eventPublisher;

    private final TransactionTemplate transactionTemplate;

    public VoiceMessageProcessor(
            MessageRepository messageRepository,
            List<ChannelMediaDownloader> downloaders,
            AudioProviderResolver audioProviderResolver,
            ApplicationEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate) {
        this.messageRepository = messageRepository;
        this.downloaders =
                downloaders.stream()
                        .collect(
                                Collectors.toMap(
                                        ChannelMediaDownloader::provider, Function.identity()));
        this.audioProviderResolver = audioProviderResolver;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInboundAudio(InboundAudioReceivedEvent event) {
        process(event);
    }

    void process(InboundAudioReceivedEvent event) {
        Optional<Source> source = transactionTemplate.execute(status -> loadSource(event));

        if (source == null || source.isEmpty()) {
            return;
        }

        byte[] audio = download(source.get(), event);
        String storageProvider = null;
        String storageKey = null;
        String transcript = null;
        TranscriptionStatus status;

        if (audio == null) {
            status = TranscriptionStatus.FAILED;
        } else {
            Optional<StorageProvider> storage =
                    audioProviderResolver.activeStorageProvider(source.get().providerSettings());

            if (storage.isPresent()) {
                storageKey = store(storage.get(), source.get(), audio);
                storageProvider = storageKey != null ? storage.get().id() : null;
            }

            Optional<TranscriptionProvider> transcription =
                    audioProviderResolver.activeTranscriptionProvider(
                            source.get().providerSettings());

            if (transcription.isEmpty()) {
                status = TranscriptionStatus.NO_PROVIDER;
            } else {
                transcript = transcribe(transcription.get(), source.get(), audio);
                status =
                        transcript != null
                                ? TranscriptionStatus.TRANSCRIBED
                                : TranscriptionStatus.FAILED;
            }
        }

        String savedStorageProvider = storageProvider;
        String savedStorageKey = storageKey;
        String savedTranscript = transcript;

        transactionTemplate.executeWithoutResult(
                transactionStatus ->
                        saveResult(
                                event,
                                savedStorageProvider,
                                savedStorageKey,
                                savedTranscript,
                                status));
    }

    private Optional<Source> loadSource(InboundAudioReceivedEvent event) {
        Optional<Message> message = messageRepository.findById(event.messageId());

        if (message.isEmpty()) {
            log.warn("Voice message not found {}", kv("messageId", event.messageId()));

            return Optional.empty();
        }

        Conversation conversation = message.get().getConversation();
        ChannelAccount channelAccount =
                (ChannelAccount) Hibernate.unproxy(conversation.getChannelAccount());

        return Optional.of(
                new Source(
                        channelAccount,
                        conversation.getWorkspace().getId(),
                        conversation.getWorkspace().getProviderSettings(),
                        message.get().getAudioMimeType()));
    }

    private byte[] download(Source source, InboundAudioReceivedEvent event) {
        ChannelMediaDownloader downloader = downloaders.get(source.channelAccount().getProvider());

        if (downloader == null) {
            log.warn(
                    "Voice message skipped, the channel can't download media {}",
                    kv("provider", source.channelAccount().getProvider()));

            return null;
        }

        try {
            byte[] audio = downloader.download(source.channelAccount(), event.mediaReference());

            if (audio.length > MAX_AUDIO_BYTES) {
                log.warn(
                        "Voice message skipped, the audio is too large {} {}",
                        kv("messageId", event.messageId()),
                        kv("bytes", audio.length));

                return null;
            }

            return audio;
        } catch (Exception exception) {
            log.warn(
                    "Voice message download failed {} {}",
                    kv("messageId", event.messageId()),
                    kv("error", exception.getMessage()));

            return null;
        }
    }

    private String store(StorageProvider storage, Source source, byte[] audio) {
        try {
            return storage.store(
                    new StorageRequest(source.workspaceId().toString(), source.mimeType(), audio));
        } catch (Exception exception) {
            log.warn(
                    "Voice message storage failed {} {}",
                    kv("provider", storage.id()),
                    kv("error", exception.getMessage()));

            return null;
        }
    }

    private String transcribe(TranscriptionProvider transcription, Source source, byte[] audio) {
        try {
            String transcript =
                    transcription.transcribe(
                            new TranscriptionRequest(
                                    audio,
                                    source.mimeType(),
                                    audioProviderResolver.transcriptionModel(
                                            source.providerSettings(), transcription.id())));

            return transcript != null && !transcript.isBlank() ? transcript.strip() : null;
        } catch (Exception exception) {
            log.warn(
                    "Voice message transcription failed {} {}",
                    kv("provider", transcription.id()),
                    kv("error", exception.getMessage()));

            return null;
        }
    }

    private void saveResult(
            InboundAudioReceivedEvent event,
            String storageProvider,
            String storageKey,
            String transcript,
            TranscriptionStatus status) {
        Optional<Message> found = messageRepository.findById(event.messageId());

        if (found.isEmpty()) {
            return;
        }

        Message message = found.get();
        message.setAudioStorageProvider(storageProvider);
        message.setAudioStorageKey(storageKey);
        message.setText(transcript);
        message.setTranscriptionStatus(status);
        messageRepository.save(message);

        Conversation conversation = message.getConversation();
        UUID workspaceId = conversation.getWorkspace().getId();

        log.info(
                "Voice message processed {} {} {}",
                kv("messageId", message.getId()),
                kv("transcriptionStatus", status),
                kv("stored", storageKey != null));

        if (transcript != null) {
            if (event.opensConversation()) {
                eventPublisher.publishEvent(new ConversationOpenedEvent(conversation, message));
            } else {
                eventPublisher.publishEvent(
                        new ConversationMessageReceivedEvent(conversation, message));
            }
        }

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.MESSAGE_CREATED,
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversation.getId().toString())));
    }

    private record Source(
            ChannelAccount channelAccount,
            UUID workspaceId,
            WorkspaceProviderSettings providerSettings,
            String mimeType) {}
}
