package com.relayflow.api.messaging;

import com.relayflow.api.configuration.CredentialEncryptionService;
import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ContactResponse;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateChannelAccountRequest;
import com.relayflow.api.messaging.dto.CreateContactRequest;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateExternalIdentityRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.CreateWorkspaceRequest;
import com.relayflow.api.messaging.dto.ExternalIdentityResponse;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.dto.PageResponse;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import com.relayflow.api.messaging.repository.ChannelAccountRepository;
import com.relayflow.api.messaging.repository.ContactRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.telegram.TelegramWebhookRegistrar;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final ChannelAccountRepository channelAccountRepository;

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final MessagingMapper mapper;

    private final ApplicationEventPublisher eventPublisher;

    private final TelegramWebhookRegistrar telegramWebhookRegistrar;

    private final CredentialEncryptionService credentialEncryptionService;

    public MessagingService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ChannelAccountRepository channelAccountRepository,
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessagingMapper mapper,
            ApplicationEventPublisher eventPublisher,
            TelegramWebhookRegistrar telegramWebhookRegistrar,
            CredentialEncryptionService credentialEncryptionService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.telegramWebhookRegistrar = telegramWebhookRegistrar;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UUID ownerId) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        log.info(
                "Workspace created: id={}, name={}, owner={}",
                workspace.getId(),
                workspace.getName(),
                ownerId);

        return mapper.toDto(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(UUID userId) {
        List<UUID> workspaceIds =
                workspaceMemberRepository.findByUserId(userId).stream()
                        .map(WorkspaceMember::getWorkspaceId)
                        .toList();

        return workspaceRepository.findAllById(workspaceIds).stream()
                .sorted(Comparator.comparing(Workspace::getCreatedAt))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelAccountResponse> listChannelAccounts(UUID workspaceId) {
        getWorkspace(workspaceId);

        return channelAccountRepository.findByWorkspace(workspaceId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ChannelAccountResponse createChannelAccount(CreateChannelAccountRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());

        // Hold plaintext for webhook registration — encrypt before persisting.
        String plainTextCredential = request.encryptedCredentials();

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setWorkspace(workspace);
        channelAccount.setProvider(request.provider());
        channelAccount.setName(request.name());
        channelAccount.setStatus(
                request.status() == null ? ChannelAccountStatus.ACTIVE : request.status());
        channelAccount.setEncryptedCredentials(
                credentialEncryptionService.encrypt(plainTextCredential));
        channelAccount.setMetadata(copyMap(request.metadata()));

        channelAccountRepository.save(channelAccount);

        if (channelAccount.getProvider() == ChannelProvider.TELEGRAM
                && plainTextCredential != null) {
            telegramWebhookRegistrar.register(plainTextCredential, channelAccount.getId());
        }

        log.info(
                "Channel account created: id={}, provider={}, workspace={}",
                channelAccount.getId(),
                channelAccount.getProvider(),
                workspace.getId());

        return mapper.toDto(channelAccount);
    }

    /**
     * Creates a Telegram channel account for a guest workspace using the shared bot token. Skips
     * webhook registration — the shared bot uses a single fixed endpoint that routes by workspace
     * ID.
     */
    @Transactional
    public void createSharedBotChannelAccount(UUID workspaceId, String botToken) {
        Workspace workspace = getWorkspace(workspaceId);

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setWorkspace(workspace);
        channelAccount.setProvider(ChannelProvider.TELEGRAM);
        channelAccount.setName("Shared Telegram Bot");
        channelAccount.setStatus(ChannelAccountStatus.ACTIVE);
        channelAccount.setEncryptedCredentials(credentialEncryptionService.encrypt(botToken));
        channelAccount.setMetadata(new LinkedHashMap<>());

        mapper.toDto(channelAccountRepository.save(channelAccount));
    }

    @Transactional
    public void disconnectChannelAccount(UUID id, UUID workspaceId) {
        ChannelAccount channelAccount =
                channelAccountRepository
                        .findInWorkspace(id, workspaceId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        channelAccount.setStatus(ChannelAccountStatus.DISABLED);
        channelAccountRepository.save(channelAccount);

        log.info(
                "Channel account disconnected: id={}, provider={}, workspace={}",
                id,
                channelAccount.getProvider(),
                workspaceId);
    }

    @Transactional
    public ChannelAccountResponse reconnectChannelAccount(UUID id, UUID workspaceId) {
        ChannelAccount channelAccount =
                channelAccountRepository
                        .findInWorkspace(id, workspaceId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        channelAccount.setStatus(ChannelAccountStatus.ACTIVE);
        channelAccountRepository.save(channelAccount);

        log.info(
                "Channel account reconnected: id={}, provider={}, workspace={}",
                id,
                channelAccount.getProvider(),
                workspaceId);

        return mapper.toDto(channelAccount);
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());

        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(request.displayName());

        return mapper.toDto(contactRepository.save(contact));
    }

    @Transactional
    public ExternalIdentityResponse createExternalIdentity(CreateExternalIdentityRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());
        Contact contact = getContact(request.contactId(), request.workspaceId());

        ExternalIdentity externalIdentity = new ExternalIdentity();
        externalIdentity.setWorkspace(workspace);
        externalIdentity.setContact(contact);
        externalIdentity.setProvider(request.provider());
        externalIdentity.setExternalUserId(request.externalUserId());
        externalIdentity.setExternalConversationId(request.externalConversationId());
        externalIdentity.setUsername(request.username());
        externalIdentity.setRawProfile(copyMap(request.rawProfile()));

        return mapper.toDto(externalIdentityRepository.save(externalIdentity));
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());
        Contact contact = getContact(request.contactId(), request.workspaceId());
        ChannelAccount channelAccount =
                getChannelAccount(request.channelAccountId(), request.workspaceId());

        Conversation conversation = new Conversation();
        conversation.setWorkspace(workspace);
        conversation.setContact(contact);
        conversation.setChannelAccount(channelAccount);
        conversation.setStatus(
                request.status() == null ? ConversationStatus.OPEN : request.status());
        conversation.setAssignedUserId(request.assignedUserId());

        return mapper.toDto(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listConversations(
            UUID workspaceId, int page, int size) {
        getWorkspace(workspaceId);

        int pageSize = Math.clamp(size, 1, 100);

        // Fetch one extra to determine hasMore without a count query.
        List<Conversation> results =
                conversationRepository.findByWorkspace(
                        workspaceId, PageRequest.of(page, pageSize + 1));

        boolean hasMore = results.size() > pageSize;
        List<Conversation> items = hasMore ? results.subList(0, pageSize) : results;

        return new PageResponse<>(items.stream().map(mapper::toDto).toList(), hasMore, null);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID workspaceId, UUID conversationId) {
        return mapper.toDto(getConversationRecord(conversationId, workspaceId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(
            UUID workspaceId, UUID conversationId, Instant before, int limit) {
        getConversationRecord(conversationId, workspaceId);

        int pageSize = Math.clamp(limit, 1, 200);

        // Fetch one extra to determine hasMore.
        List<Message> raw =
                before == null
                        ? messageRepository.findRecentByConversation(
                                conversationId, workspaceId, PageRequest.of(0, pageSize + 1))
                        : messageRepository.findBeforeCursor(
                                conversationId,
                                workspaceId,
                                before,
                                PageRequest.of(0, pageSize + 1));

        boolean hasMore = raw.size() > pageSize;
        List<Message> page =
                hasMore ? new ArrayList<>(raw.subList(0, pageSize)) : new ArrayList<>(raw);

        // Results are newest-first from the DB; reverse to present oldest-first to clients.
        Collections.reverse(page);

        // nextCursor = the oldest message's createdAt so the client can load messages before it.
        String nextCursor =
                hasMore && !page.isEmpty() ? page.getFirst().getCreatedAt().toString() : null;

        return new PageResponse<>(page.stream().map(mapper::toDto).toList(), hasMore, nextCursor);
    }

    @Transactional
    public MessageResponse createMessage(
            UUID workspaceId, UUID conversationId, CreateMessageRequest request) {
        Conversation conversation = getConversationRecord(conversationId, workspaceId);

        Message message = new Message();
        message.setWorkspace(conversation.getWorkspace());
        message.setConversation(conversation);
        message.setDirection(request.direction());
        message.setSenderType(request.senderType());
        message.setText(request.text());
        message.setProviderMessageId(request.providerMessageId());
        message.setRawPayload(copyMap(request.rawPayload()));

        Message saved = messageRepository.save(message);
        conversation.setLastMessageAt(
                saved.getCreatedAt() == null ? Instant.now() : saved.getCreatedAt());
        conversationRepository.save(conversation);

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "message.created",
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversationId.toString())));

        if (saved.getDirection() == MessageDirection.OUTBOUND) {
            log.debug(
                    "Publishing outbound event: messageId={}, conversationId={}",
                    saved.getId(),
                    conversationId);

            eventPublisher.publishEvent(
                    new OutboundMessageEvent(saved, conversation.getChannelAccount()));
        }

        return mapper.toDto(saved);
    }

    /**
     * Soft-deletes all data belonging to a workspace, then the workspace itself. No rows are
     * permanently removed — every table's {@code deleted_at} is stamped with the same instant. FK
     * order no longer matters for soft deletes, but preserved for clarity.
     */
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        log.info("Soft-deleting workspace and all associated data: {}", workspaceId);
        Instant now = Instant.now();
        messageRepository.softDeleteByWorkspaceId(workspaceId, now);
        conversationRepository.softDeleteByWorkspaceId(workspaceId, now);
        externalIdentityRepository.softDeleteByWorkspaceId(workspaceId, now);
        contactRepository.softDeleteByWorkspaceId(workspaceId, now);
        channelAccountRepository.softDeleteByWorkspaceId(workspaceId, now);
        workspaceMemberRepository.softDeleteByWorkspaceId(workspaceId, now);
        workspaceRepository.deleteById(workspaceId);
    }

    // --- lookup helpers ---

    private Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    private Contact getContact(UUID contactId, UUID workspaceId) {
        Contact contact =
                contactRepository
                        .findById(contactId)
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Contact not found");
        }

        return contact;
    }

    private ChannelAccount getChannelAccount(UUID channelAccountId, UUID workspaceId) {
        ChannelAccount channelAccount =
                channelAccountRepository
                        .findById(channelAccountId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        if (!channelAccount.getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Channel account not found");
        }

        return channelAccount;
    }

    private Conversation getConversationRecord(UUID conversationId, UUID workspaceId) {
        return conversationRepository
                .findInWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
