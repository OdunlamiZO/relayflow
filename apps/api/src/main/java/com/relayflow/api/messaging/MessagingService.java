package com.relayflow.api.messaging;

import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.ChannelAccountStatus;
import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ContactDetailResponse;
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
import com.relayflow.api.messaging.dto.WorkspaceMemberResponse;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import com.relayflow.api.messaging.repository.ChannelAccountRepository;
import com.relayflow.api.messaging.repository.ContactRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.subscription.SubscriptionService;
import com.relayflow.api.subscription.domain.LimitType;
import com.relayflow.api.telegram.TelegramWebhookRegistrar;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import com.relayflow.api.workflow.repository.WorkflowRunStepRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final UserRepository userRepository;

    private final ChannelAccountRepository channelAccountRepository;

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final MessagingMapper mapper;

    private final ApplicationEventPublisher eventPublisher;

    private final TelegramWebhookRegistrar telegramWebhookRegistrar;

    private final CredentialEncryptionService credentialEncryptionService;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final WorkflowRunStepRepository workflowRunStepRepository;

    private final SubscriptionService subscriptionService;

    private final AiAgentConfigurationRepository aiAgentConfigurationRepository;

    private final AiAgentInvocationLogRepository aiAgentInvocationLogRepository;

    private final ConversationAiDraftRepository conversationAiDraftRepository;

    private final String sharedBotToken;

    public MessagingService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            ChannelAccountRepository channelAccountRepository,
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessagingMapper mapper,
            ApplicationEventPublisher eventPublisher,
            TelegramWebhookRegistrar telegramWebhookRegistrar,
            CredentialEncryptionService credentialEncryptionService,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowRunRepository workflowRunRepository,
            WorkflowRunStepRepository workflowRunStepRepository,
            SubscriptionService subscriptionService,
            AiAgentConfigurationRepository aiAgentConfigurationRepository,
            AiAgentInvocationLogRepository aiAgentInvocationLogRepository,
            ConversationAiDraftRepository conversationAiDraftRepository,
            @Value("${shared.telegram.bot-token:}") String sharedBotToken) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.telegramWebhookRegistrar = telegramWebhookRegistrar;
        this.credentialEncryptionService = credentialEncryptionService;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.workflowRunStepRepository = workflowRunStepRepository;
        this.subscriptionService = subscriptionService;
        this.aiAgentConfigurationRepository = aiAgentConfigurationRepository;
        this.aiAgentInvocationLogRepository = aiAgentInvocationLogRepository;
        this.conversationAiDraftRepository = conversationAiDraftRepository;
        this.sharedBotToken = sharedBotToken;
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

        subscriptionService.createFreeSubscription(workspace);

        log.info(
                "Workspace created: id={}, name={}, owner={}",
                workspace.getId(),
                workspace.getName(),
                ownerId);

        return toWorkspaceResponse(workspace);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, String name) {
        Workspace workspace = getWorkspace(workspaceId);
        workspace.setName(name);
        workspaceRepository.save(workspace);

        return toWorkspaceResponse(workspace);
    }

    /**
     * Creates a workspace for a guest user and, if the shared bot is configured, attaches it as a
     * channel account in the same transaction. If channel-account creation fails, the workspace
     * creation is rolled back, so the caller never receives a workspace ID that has no bot
     * attached.
     */
    @Transactional
    public WorkspaceResponse createGuestWorkspace(CreateWorkspaceRequest request, UUID ownerId) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        subscriptionService.createFreeSubscription(workspace);

        if (sharedBotToken != null && !sharedBotToken.isBlank()) {
            ChannelAccount channelAccount = new ChannelAccount();
            channelAccount.setWorkspace(workspace);
            channelAccount.setProvider(ChannelProvider.TELEGRAM);
            channelAccount.setName("Shared Telegram Bot");
            channelAccount.setStatus(ChannelAccountStatus.ACTIVE);
            channelAccount.setShared(true);
            channelAccount.setEncryptedCredentials(
                    credentialEncryptionService.encrypt(sharedBotToken));
            channelAccount.setMetadata(new LinkedHashMap<>());
            channelAccountRepository.save(channelAccount);
        }

        log.info(
                "Guest workspace created: id={}, name={}, owner={}",
                workspace.getId(),
                workspace.getName(),
                ownerId);

        return toWorkspaceResponse(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(UUID userId) {
        List<UUID> workspaceIds =
                workspaceMemberRepository.findByUser(userId).stream()
                        .map(WorkspaceMember::getWorkspaceId)
                        .toList();

        return workspaceRepository.findAllById(workspaceIds).stream()
                .sorted(Comparator.comparing(Workspace::getCreatedAt))
                .map(this::toWorkspaceResponse)
                .toList();
    }

    /**
     * Maps a workspace to its response DTO, including whether someone has linked the shared
     * Telegram bot — derived from {@link ExternalIdentity} rather than stored on the workspace
     * itself, so it stays correct across page refreshes.
     */
    private WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        WorkspaceResponse base = mapper.toDto(workspace);
        boolean telegramLinked =
                externalIdentityRepository.existsSharedTelegramLinkForWorkspace(workspace.getId());

        return new WorkspaceResponse(base.id(), base.name(), base.createdAt(), telegramLinked);
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

        if (workspaceMemberRepository.countByWorkspace(request.workspaceId(), true) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Guest workspaces cannot connect additional channels. Sign up to add your"
                            + " own channels.");
        }

        // Enforce plan limit before creating the channel account.
        long count = channelAccountRepository.countBillable(request.workspaceId(), null);
        subscriptionService.enforceLimit(request.workspaceId(), LimitType.CHANNEL_ACCOUNTS, count);

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
            String webhookSecret =
                    telegramWebhookRegistrar.register(plainTextCredential, channelAccount.getId());
            channelAccount.setWebhookSecret(webhookSecret);
            channelAccountRepository.save(channelAccount);
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
        channelAccount.setShared(true);
        channelAccount.setEncryptedCredentials(credentialEncryptionService.encrypt(botToken));
        channelAccount.setMetadata(new LinkedHashMap<>());

        mapper.toDto(channelAccountRepository.save(channelAccount));
    }

    /**
     * Removes the workspace's shared Telegram bot channel account, along with any conversations,
     * messages and workflow runs that happened over it. Called when a guest account converts to a
     * real one — the shared bot is only for trying out the product as a guest, so none of that test
     * traffic carries into the new account (the workflow definitions themselves are kept).
     */
    @Transactional
    public void dropSharedTelegramChannel(UUID workspaceId) {
        channelAccountRepository.findSharedByWorkspace(workspaceId).stream()
                .filter(ca -> ca.getProvider() == ChannelProvider.TELEGRAM)
                .forEach(
                        ca -> {
                            List<Conversation> conversations =
                                    conversationRepository.findByChannelAccount(ca.getId());
                            List<UUID> conversationIds =
                                    conversations.stream().map(Conversation::getId).toList();
                            List<UUID> contactIds =
                                    conversations.stream()
                                            .map(conversation -> conversation.getContact().getId())
                                            .distinct()
                                            .toList();

                            if (!conversationIds.isEmpty()) {
                                workflowRunStepRepository.deleteByRunConversations(conversationIds);
                                workflowRunRepository.deleteByConversations(conversationIds);
                                messageRepository.deleteByConversations(conversationIds);
                            }

                            conversationRepository.deleteByChannelAccount(ca.getId());
                            externalIdentityRepository.deleteByChannelAccount(ca.getId());
                            channelAccountRepository.delete(ca);

                            if (!contactIds.isEmpty()) {
                                contactRepository.deleteOrphaned(contactIds);
                            }
                        });
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

        subscriptionService.enforceLimitOnEnable(workspaceId, LimitType.CHANNEL_ACCOUNTS);

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

        ContactResponse base = mapper.toDto(contactRepository.save(contact));

        return new ContactResponse(
                base.id(), base.workspaceId(), base.displayName(), base.createdAt(), List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> listContacts(UUID workspaceId, int page, int size) {
        getWorkspace(workspaceId);

        int pageSize = Math.clamp(size, 1, 100);

        List<Contact> results =
                contactRepository.findByWorkspace(workspaceId, PageRequest.of(page, pageSize + 1));

        boolean hasMore = results.size() > pageSize;
        List<Contact> items = hasMore ? results.subList(0, pageSize) : results;

        List<UUID> contactIds = items.stream().map(Contact::getId).toList();
        Map<UUID, List<ExternalIdentityResponse>> identitiesByContact =
                externalIdentityRepository.findByContacts(contactIds).stream()
                        .map(mapper::toDto)
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        ExternalIdentityResponse::contactId));

        List<ContactResponse> responses =
                items.stream()
                        .map(
                                c -> {
                                    ContactResponse base = mapper.toDto(c);
                                    List<ExternalIdentityResponse> ids =
                                            identitiesByContact.getOrDefault(c.getId(), List.of());

                                    return new ContactResponse(
                                            base.id(),
                                            base.workspaceId(),
                                            base.displayName(),
                                            base.createdAt(),
                                            ids);
                                })
                        .toList();

        return new PageResponse<>(responses, hasMore, null);
    }

    @Transactional(readOnly = true)
    public ContactDetailResponse getContactDetail(UUID contactId, UUID workspaceId) {
        Contact contact = getContact(contactId, workspaceId);

        List<ExternalIdentityResponse> identities =
                externalIdentityRepository.findByContact(contactId).stream()
                        .map(mapper::toDto)
                        .toList();

        return new ContactDetailResponse(
                contact.getId(),
                workspaceId,
                contact.getDisplayName(),
                contact.getCreatedAt(),
                identities);
    }

    @Transactional
    public void deleteContact(UUID id, UUID workspaceId) {
        Contact contact =
                contactRepository
                        .findById(id)
                        .filter(c -> c.getWorkspace().getId().equals(workspaceId))
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contactRepository.delete(contact);

        log.info("Contact deleted: id={}, workspace={}", id, workspaceId);
    }

    @Transactional
    public ExternalIdentityResponse createExternalIdentity(CreateExternalIdentityRequest request) {
        Workspace workspace = getWorkspace(request.workspaceId());
        Contact contact = getContact(request.contactId(), request.workspaceId());

        ChannelAccount channelAccount =
                request.channelAccountId() != null
                        ? getChannelAccount(request.channelAccountId(), request.workspaceId())
                        : null;

        ExternalIdentity externalIdentity = new ExternalIdentity();
        externalIdentity.setWorkspace(workspace);
        externalIdentity.setContact(contact);
        externalIdentity.setChannelAccount(channelAccount);
        externalIdentity.setProvider(request.provider());
        externalIdentity.setExternalUserId(request.externalUserId());
        externalIdentity.setExternalConversationId(request.externalConversationId());
        externalIdentity.setUsername(request.username());
        externalIdentity.setRawProfile(copyMap(request.rawProfile()));

        return mapper.toDto(externalIdentityRepository.save(externalIdentity));
    }

    @Transactional
    public ContactResponse mergeContacts(UUID targetId, UUID sourceId, UUID workspaceId) {
        if (targetId.equals(sourceId)) {
            throw new IllegalArgumentException("A contact cannot be merged with itself");
        }

        Contact target = getContact(targetId, workspaceId);
        Contact source = getContact(sourceId, workspaceId);

        // Guard: reject if both contacts have an identity on the same channel account —
        // that would mean two different external users on the same bot are being claimed
        // as the same person, which is never valid.
        List<ExternalIdentity> sourceIdentities =
                externalIdentityRepository.findByContact(sourceId);
        List<ExternalIdentity> targetIdentities =
                externalIdentityRepository.findByContact(targetId);

        java.util.Set<UUID> targetChannelAccountIds =
                targetIdentities.stream()
                        .filter(identity -> identity.getChannelAccount() != null)
                        .map(identity -> identity.getChannelAccount().getId())
                        .collect(java.util.stream.Collectors.toSet());

        for (ExternalIdentity identity : sourceIdentities) {
            if (identity.getChannelAccount() != null
                    && targetChannelAccountIds.contains(identity.getChannelAccount().getId())) {
                throw new IllegalArgumentException(
                        "Cannot merge: both contacts have identities on the same channel account");
            }
        }

        // Bulk-reassign identities and conversations to the target contact.
        externalIdentityRepository.reassignContact(target, sourceId);
        conversationRepository.reassignContact(target, sourceId);

        contactRepository.delete(source);

        log.info(
                "Contacts merged — target={} source={} workspace={}",
                targetId,
                sourceId,
                workspaceId);

        // Return the updated target with all newly merged identities.
        List<UUID> contactIds = List.of(targetId);
        Map<UUID, List<ExternalIdentityResponse>> identitiesByContact =
                externalIdentityRepository.findByContacts(contactIds).stream()
                        .map(mapper::toDto)
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        ExternalIdentityResponse::contactId));

        List<ExternalIdentityResponse> mergedIdentities =
                identitiesByContact.getOrDefault(targetId, List.of());
        ContactResponse base = mapper.toDto(target);

        return new ContactResponse(
                base.id(),
                base.workspaceId(),
                base.displayName(),
                base.createdAt(),
                mergedIdentities);
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
        conversation.setAssigneeId(request.assigneeId());

        return mapper.toDto(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listConversations(
            UUID workspaceId, UUID contactId, UUID channelAccountId, int page, int size) {
        getWorkspace(workspaceId);

        int pageSize = Math.clamp(size, 1, 100);

        // Fetch one extra to determine hasMore without a count query.
        List<Conversation> results;

        if (contactId != null && channelAccountId != null) {
            results =
                    conversationRepository.findLatestConversationForContact(
                            workspaceId,
                            channelAccountId,
                            contactId,
                            PageRequest.of(page, pageSize + 1));
        } else if (contactId != null) {
            results =
                    conversationRepository.findByWorkspaceAndContact(
                            workspaceId, contactId, PageRequest.of(page, pageSize + 1));
        } else {
            results =
                    conversationRepository.findByWorkspace(
                            workspaceId, PageRequest.of(page, pageSize + 1));
        }

        boolean hasMore = results.size() > pageSize;
        List<Conversation> items = hasMore ? results.subList(0, pageSize) : results;

        return new PageResponse<>(items.stream().map(mapper::toDto).toList(), hasMore, null);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID workspaceId, UUID conversationId) {
        return mapper.toDto(getConversationRecord(conversationId, workspaceId));
    }

    @Transactional
    public ConversationResponse updateConversationStatus(
            UUID workspaceId, UUID conversationId, ConversationStatus status) {
        Conversation conversation = getConversationRecord(conversationId, workspaceId);
        conversation.setStatus(status);

        // A closed conversation no longer needs an owner — unassign it so it doesn't
        // linger in someone's queue.
        if (status == ConversationStatus.CLOSED) {
            conversation.setAssigneeId(null);
        }

        return mapper.toDto(conversationRepository.save(conversation));
    }

    @Transactional
    public ConversationResponse updateConversationAssignee(
            UUID workspaceId, UUID conversationId, UUID assigneeId) {
        Conversation conversation = getConversationRecord(conversationId, workspaceId);

        if (assigneeId != null
                && workspaceMemberRepository
                        .findByWorkspaceAndUser(workspaceId, assigneeId)
                        .isEmpty()) {
            throw new ResourceNotFoundException("Workspace member not found");
        }

        conversation.setAssigneeId(assigneeId);

        return mapper.toDto(conversationRepository.save(conversation));
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
            UUID workspaceId,
            UUID conversationId,
            CreateMessageRequest request,
            UUID senderUserId) {
        Conversation conversation = getConversationRecord(conversationId, workspaceId);

        // Workflow ownership check — only workflow executors may message while a workflow is
        // active.
        if (conversation.isLockedByWorkflow()) {
            throw new ConversationLockedException(
                    "A workflow is currently handling this conversation. "
                            + "Wait for the workflow to finish before replying.");
        }

        boolean reopened = false;

        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setAssigneeId(null);
            conversation.setSessionStartedAt(Instant.now());
            reopened = true;
        }

        // Auto-assign an unassigned conversation to the agent who first replies to it.
        boolean autoAssigned = false;

        if (request.direction() == MessageDirection.OUTBOUND
                && request.senderType() == MessageSenderType.AGENT
                && conversation.getAssigneeId() == null
                && senderUserId != null) {
            conversation.setAssigneeId(senderUserId);
            autoAssigned = true;
        }

        Message message = new Message();
        message.setWorkspace(conversation.getWorkspace());
        message.setConversation(conversation);
        message.setDirection(request.direction());
        message.setSenderType(request.senderType());
        message.setText(request.text());
        message.setProviderMessageId(request.providerMessageId());
        message.setRawPayload(copyMap(request.rawPayload()));

        message = messageRepository.save(message);
        conversation.setLastMessageAt(
                message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt());
        conversationRepository.save(conversation);

        if (reopened || autoAssigned) {
            eventPublisher.publishEvent(
                    new SseBroadcastEvent(
                            workspaceId,
                            "conversation.updated",
                            Map.of(
                                    "workspaceId", workspaceId.toString(),
                                    "conversationId", conversationId.toString())));
        }

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "message.created",
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversationId.toString())));

        if (message.getDirection() == MessageDirection.OUTBOUND) {
            log.debug(
                    "Publishing outbound event: messageId={}, conversationId={}",
                    message.getId(),
                    conversationId);

            eventPublisher.publishEvent(
                    new OutboundMessageEvent(message, conversation.getChannelAccount()));

            if (message.getSenderType() == MessageSenderType.AGENT) {
                conversationAiDraftRepository.deleteByConversationId(conversationId);
                conversation.setLockedByAiAgent(false);
            }
        }

        return mapper.toDto(message);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listWorkspaceMembers(UUID workspaceId) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace(workspaceId);

        Map<UUID, User> userMap =
                userRepository
                        .findAllById(members.stream().map(WorkspaceMember::getUserId).toList())
                        .stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream()
                .map(member -> toMemberResponse(member, userMap.get(member.getUserId())))
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse inviteWorkspaceMember(
            UUID workspaceId, String email, Set<WorkspacePermission> permissions) {
        if (workspaceMemberRepository.countByWorkspace(workspaceId, true) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Guest workspaces cannot invite members");
        }

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "No user found with email: " + email));

        if (workspaceMemberRepository
                .findByWorkspaceAndUser(workspaceId, user.getId())
                .isPresent()) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        WorkspacePermission.validateDependencies(permissions);

        long count = workspaceMemberRepository.countByWorkspace(workspaceId, null);
        subscriptionService.enforceLimit(workspaceId, LimitType.MEMBERS_PER_WORKSPACE, count);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getId());
        member.setRole(WorkspaceRole.MEMBER);
        member.setPermissions(permissions);
        workspaceMemberRepository.save(member);

        log.info("Member invited: userId={}, workspace={}", user.getId(), workspaceId);

        return toMemberResponse(member, user);
    }

    @Transactional
    public WorkspaceMemberResponse updateWorkspaceMember(
            UUID workspaceId,
            UUID memberId,
            WorkspaceRole role,
            Set<WorkspacePermission> permissions) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Guard: ownership can only be transferred, never set directly.
        if (role == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException(
                    "Ownership can only be transferred via the transfer-ownership endpoint");
        }

        // Guard: the owner cannot be demoted — workspaces have exactly one owner.
        if (member.getRole() == WorkspaceRole.OWNER && role != null) {
            throw new IllegalArgumentException("Cannot demote the owner of the workspace");
        }

        WorkspacePermission.validateDependencies(permissions);

        if (role != null) {
            member.setRole(role);
        }

        member.setPermissions(permissions);
        workspaceMemberRepository.save(member);

        User user = userRepository.findById(member.getUserId()).orElse(null);

        return toMemberResponse(member, user);
    }

    @Transactional
    public void removeWorkspaceMember(UUID workspaceId, UUID memberId) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Guard: the owner cannot be removed — workspaces have exactly one owner.
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the owner of the workspace");
        }

        workspaceMemberRepository.delete(member);

        log.info("Member removed: id={}, workspace={}", memberId, workspaceId);
    }

    /**
     * Transfers ownership of the workspace to another member. The current owner is demoted to
     * {@link WorkspaceRole#MEMBER}; the target member becomes {@link WorkspaceRole#OWNER}.
     *
     * <p>Only the current owner can call this; {@code callerId} must be their user ID.
     */
    @Transactional
    public void transferOwnership(UUID workspaceId, UUID newOwnerMemberId, UUID callerId) {
        WorkspaceMember currentOwner =
                workspaceMemberRepository
                        .findByWorkspaceAndUser(workspaceId, callerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (currentOwner.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the workspace owner can transfer ownership");
        }

        WorkspaceMember newOwner =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, newOwnerMemberId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Target member not found"));

        if (newOwner.getId().equals(currentOwner.getId())) {
            throw new IllegalArgumentException("Cannot transfer ownership to yourself");
        }

        currentOwner.setRole(WorkspaceRole.MEMBER);
        newOwner.setRole(WorkspaceRole.OWNER);

        workspaceMemberRepository.save(currentOwner);
        workspaceMemberRepository.save(newOwner);

        log.info(
                "Ownership transferred: workspace={}, from={}, to={}",
                workspaceId,
                currentOwner.getId(),
                newOwner.getId());
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
        messageRepository.softDeleteByWorkspace(workspaceId, now);
        conversationRepository.softDeleteByWorkspace(workspaceId, now);
        externalIdentityRepository.softDeleteByWorkspace(workspaceId, now);
        contactRepository.softDeleteByWorkspace(workspaceId, now);
        channelAccountRepository.softDeleteByWorkspace(workspaceId, now);
        workflowDefinitionRepository.softDeleteByWorkspace(workspaceId, now);
        workspaceMemberRepository.softDeleteByWorkspace(workspaceId, now);
        conversationAiDraftRepository.deleteByWorkspaceId(workspaceId);
        aiAgentInvocationLogRepository.deleteByWorkspaceId(workspaceId);
        aiAgentConfigurationRepository.deleteByWorkspaceId(workspaceId);
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

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember member, User user) {

        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUserId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getDisplayName() : null,
                user != null ? user.getAvatarUrl() : null,
                member.getRole(),
                member.getPermissions(),
                member.getJoinedAt());
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
