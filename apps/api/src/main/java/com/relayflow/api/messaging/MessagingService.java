package com.relayflow.api.messaging;

import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.channel.ChannelAccountService;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.common.dto.PageResponse;
import com.relayflow.api.contact.ContactService;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.CreateConversationRequest;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceMemberRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final MessagingMapper mapper;

    private final WorkspaceService workspaceService;

    private final ContactService contactService;

    private final ChannelAccountService channelAccountService;

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final ConversationAiDraftRepository conversationAiDraftRepository;

    public MessagingService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessagingMapper mapper,
            WorkspaceService workspaceService,
            ContactService contactService,
            ChannelAccountService channelAccountService,
            WorkspaceMemberRepository workspaceMemberRepository,
            ApplicationEventPublisher eventPublisher,
            ConversationAiDraftRepository conversationAiDraftRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.workspaceService = workspaceService;
        this.contactService = contactService;
        this.channelAccountService = channelAccountService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.eventPublisher = eventPublisher;
        this.conversationAiDraftRepository = conversationAiDraftRepository;
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspaceId());
        Contact contact = contactService.getContact(request.contactId(), request.workspaceId());
        ChannelAccount channelAccount =
                channelAccountService.getChannelAccount(
                        request.channelAccountId(), request.workspaceId());

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
        workspaceService.getWorkspace(workspaceId);

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

        if (request.direction() == MessageDirection.OUTBOUND
                && request.senderType() == MessageSenderType.AGENT
                && conversation.getEscalatedAt() != null) {
            conversation.setEscalatedAt(null);
            conversation.setEscalationReason(null);
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
                            SseEventType.CONVERSATION_UPDATED,
                            Map.of(
                                    "workspaceId", workspaceId.toString(),
                                    "conversationId", conversationId.toString())));
        }

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.MESSAGE_CREATED,
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

    private Conversation getConversationRecord(UUID conversationId, UUID workspaceId) {
        return conversationRepository
                .findInWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
