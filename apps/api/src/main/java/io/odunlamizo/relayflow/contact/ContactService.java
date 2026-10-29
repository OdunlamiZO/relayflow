package io.odunlamizo.relayflow.contact;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.channel.ChannelAccountService;
import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.common.MapUtils;
import io.odunlamizo.relayflow.common.dto.PageResponse;
import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.domain.ExternalIdentity;
import io.odunlamizo.relayflow.contact.dto.ContactDetailResponse;
import io.odunlamizo.relayflow.contact.dto.ContactResponse;
import io.odunlamizo.relayflow.contact.dto.CreateContactRequest;
import io.odunlamizo.relayflow.contact.dto.CreateExternalIdentityRequest;
import io.odunlamizo.relayflow.contact.dto.ExternalIdentityResponse;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.contact.repository.ExternalIdentityRepository;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.sse.SseBroadcastEvent;
import io.odunlamizo.relayflow.sse.SseEventType;
import io.odunlamizo.relayflow.webhook.ContactSnapshotBuilder;
import io.odunlamizo.relayflow.webhook.WebhookDispatchService;
import io.odunlamizo.relayflow.webhook.domain.WebhookEventType;
import io.odunlamizo.relayflow.webhook.dto.ContactMergedPayload;
import io.odunlamizo.relayflow.webhook.dto.ContactSnapshot;
import io.odunlamizo.relayflow.workflow.domain.WorkflowRunStatus;
import io.odunlamizo.relayflow.workflow.repository.WorkflowRunRepository;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ConversationRepository conversationRepository;

    private final ContactMapper mapper;

    private final WorkspaceService workspaceService;

    private final ChannelAccountService channelAccountService;

    private final ReservedContactFieldResolver reservedContactFieldResolver;

    private final WebhookDispatchService webhookDispatchService;

    private final ContactTagService contactTagService;

    private final MessageRepository messageRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final ConversationAiDraftRepository conversationAiDraftRepository;

    private final ApplicationEventPublisher eventPublisher;

    public ContactService(
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            ContactMapper mapper,
            WorkspaceService workspaceService,
            ChannelAccountService channelAccountService,
            ReservedContactFieldResolver reservedContactFieldResolver,
            WebhookDispatchService webhookDispatchService,
            ContactTagService contactTagService,
            MessageRepository messageRepository,
            WorkflowRunRepository workflowRunRepository,
            ConversationAiDraftRepository conversationAiDraftRepository,
            ApplicationEventPublisher eventPublisher) {
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.mapper = mapper;
        this.workspaceService = workspaceService;
        this.channelAccountService = channelAccountService;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
        this.webhookDispatchService = webhookDispatchService;
        this.contactTagService = contactTagService;
        this.messageRepository = messageRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.conversationAiDraftRepository = conversationAiDraftRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspaceId());

        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(request.displayName());

        ContactResponse base = mapper.toDto(contactRepository.save(contact));

        return withIdentities(base, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> listContacts(UUID workspaceId, int page, int size) {
        workspaceService.getWorkspace(workspaceId);

        int pageSize = Math.clamp(size, 1, 100);

        List<Contact> results =
                contactRepository.findByWorkspace(workspaceId, PageRequest.of(page, pageSize + 1));

        boolean hasMore = results.size() > pageSize;
        List<Contact> items = hasMore ? results.subList(0, pageSize) : results;

        List<UUID> contactIds = items.stream().map(Contact::getId).toList();
        Map<UUID, List<ExternalIdentityResponse>> identitiesByContact =
                externalIdentityRepository.findByContacts(contactIds).stream()
                        .map(mapper::toDto)
                        .collect(Collectors.groupingBy(ExternalIdentityResponse::contactId));

        List<ContactResponse> responses =
                items.stream()
                        .map(
                                contact -> {
                                    ContactResponse base = mapper.toDto(contact);
                                    List<ExternalIdentityResponse> ids =
                                            identitiesByContact.getOrDefault(
                                                    contact.getId(), List.of());

                                    return withIdentities(base, ids);
                                })
                        .toList();

        return new PageResponse<>(responses, hasMore, null);
    }

    @Transactional(readOnly = true)
    public ContactDetailResponse getContactDetail(UUID contactId, UUID workspaceId) {
        Contact contact = getContact(contactId, workspaceId);

        List<ExternalIdentity> externalIdentities =
                externalIdentityRepository.findByContact(contactId);
        List<ExternalIdentityResponse> identities =
                externalIdentities.stream().map(mapper::toDto).toList();

        Map<String, String> customFields =
                new LinkedHashMap<>(
                        reservedContactFieldResolver.resolve(contact, externalIdentities));
        customFields.putAll(contact.getCustomFields());

        return new ContactDetailResponse(
                contact.getId(),
                workspaceId,
                contact.getDisplayName(),
                customFields,
                contact.getTags(),
                contact.getCreatedAt(),
                identities);
    }

    @Transactional
    public ContactDetailResponse updateContactCustomFields(
            UUID contactId, UUID workspaceId, Map<String, String> customFields) {
        Contact contact = getContact(contactId, workspaceId);
        contact.setCustomFields(customFields);
        ContactDisplayNameSync.apply(contact);
        contactRepository.save(contact);

        webhookDispatchService.dispatch(
                workspaceId,
                WebhookEventType.CONTACT_UPDATED,
                ContactSnapshotBuilder.build(contact));

        return getContactDetail(contactId, workspaceId);
    }

    @Transactional
    public ContactDetailResponse updateContactTags(
            UUID contactId, UUID workspaceId, Map<String, String> tags) {
        contactTagService.applyTags(getContact(contactId, workspaceId), tags);

        return getContactDetail(contactId, workspaceId);
    }

    @Transactional
    public void deleteContact(UUID id, UUID workspaceId) {
        Contact contact = getContact(id, workspaceId);
        ContactSnapshot snapshot = ContactSnapshotBuilder.build(contact);
        List<UUID> conversationIds = conversationRepository.findIdsByContact(id);
        Instant now = Instant.now();

        if (!conversationIds.isEmpty()) {
            conversationAiDraftRepository.deleteByConversations(conversationIds);
            workflowRunRepository.failActiveForConversations(
                    conversationIds,
                    List.of(WorkflowRunStatus.RUNNING, WorkflowRunStatus.WAITING),
                    WorkflowRunStatus.FAILED,
                    "Contact was deleted",
                    now);
            messageRepository.softDeleteByConversations(conversationIds, now);
            conversationRepository.softDeleteByContact(id, now);
        }

        externalIdentityRepository.softDeleteByContact(id, now);
        contactRepository.delete(contact);
        webhookDispatchService.dispatch(workspaceId, WebhookEventType.CONTACT_DELETED, snapshot);

        conversationIds.forEach(
                conversationId ->
                        eventPublisher.publishEvent(
                                new SseBroadcastEvent(
                                        workspaceId,
                                        SseEventType.CONVERSATION_UPDATED,
                                        Map.of(
                                                "workspaceId", workspaceId.toString(),
                                                "conversationId", conversationId.toString()))));

        log.info(
                "Contact deleted {} {} {}",
                kv("workspaceId", workspaceId),
                kv("contactId", id),
                kv("conversationCount", conversationIds.size()));
    }

    @Transactional
    public ExternalIdentityResponse createExternalIdentity(CreateExternalIdentityRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspaceId());
        Contact contact = getContact(request.contactId(), request.workspaceId());

        ChannelAccount channelAccount =
                request.channelAccountId() != null
                        ? channelAccountService.getChannelAccount(
                                request.channelAccountId(), request.workspaceId())
                        : null;

        ExternalIdentity externalIdentity = new ExternalIdentity();
        externalIdentity.setWorkspace(workspace);
        externalIdentity.setContact(contact);
        externalIdentity.setChannelAccount(channelAccount);
        externalIdentity.setProvider(request.provider());
        externalIdentity.setExternalUserId(request.externalUserId());
        externalIdentity.setExternalConversationId(request.externalConversationId());
        externalIdentity.setUsername(request.username());
        externalIdentity.setRawProfile(MapUtils.copyMap(request.rawProfile()));

        return mapper.toDto(externalIdentityRepository.save(externalIdentity));
    }

    @Transactional
    public ContactResponse mergeContacts(UUID targetId, UUID sourceId, UUID workspaceId) {
        if (targetId.equals(sourceId)) {
            throw new IllegalArgumentException("A contact cannot be merged with itself");
        }

        Contact target = getContact(targetId, workspaceId);
        Contact source = getContact(sourceId, workspaceId);

        // Two identities on one channel account are different users and cannot be merged.
        List<ExternalIdentity> sourceIdentities =
                externalIdentityRepository.findByContact(sourceId);
        List<ExternalIdentity> targetIdentities =
                externalIdentityRepository.findByContact(targetId);

        Set<UUID> targetChannelAccountIds =
                targetIdentities.stream()
                        .filter(identity -> identity.getChannelAccount() != null)
                        .map(identity -> identity.getChannelAccount().getId())
                        .collect(Collectors.toSet());

        for (ExternalIdentity identity : sourceIdentities) {
            if (identity.getChannelAccount() != null
                    && targetChannelAccountIds.contains(identity.getChannelAccount().getId())) {
                throw new IllegalArgumentException(
                        "Cannot merge: both contacts have identities on the same channel account");
            }
        }

        externalIdentityRepository.reassignContact(target, sourceId);
        conversationRepository.reassignContact(target, sourceId);

        Map<String, Object> mergedContact = ContactSnapshotBuilder.build(source).contact();
        contactRepository.delete(source);
        webhookDispatchService.dispatch(
                workspaceId,
                WebhookEventType.CONTACT_MERGED,
                new ContactMergedPayload(
                        ContactSnapshotBuilder.build(target).contact(), mergedContact));

        log.info(
                "Contacts merged {} {} {}",
                kv("workspaceId", workspaceId),
                kv("targetContactId", targetId),
                kv("sourceContactId", sourceId));

        List<UUID> contactIds = List.of(targetId);
        Map<UUID, List<ExternalIdentityResponse>> identitiesByContact =
                externalIdentityRepository.findByContacts(contactIds).stream()
                        .map(mapper::toDto)
                        .collect(Collectors.groupingBy(ExternalIdentityResponse::contactId));

        List<ExternalIdentityResponse> mergedIdentities =
                identitiesByContact.getOrDefault(targetId, List.of());
        ContactResponse base = mapper.toDto(target);

        return withIdentities(base, mergedIdentities);
    }

    private static ContactResponse withIdentities(
            ContactResponse base, List<ExternalIdentityResponse> identities) {
        return new ContactResponse(
                base.id(),
                base.workspaceId(),
                base.displayName(),
                base.customFields(),
                base.tags(),
                base.createdAt(),
                identities);
    }

    public Contact getContact(UUID contactId, UUID workspaceId) {
        Contact contact =
                contactRepository.findById(contactId).orElseThrow(ContactNotFoundException::new);

        if (!contact.getWorkspace().getId().equals(workspaceId)) {
            throw new ContactNotFoundException();
        }

        return contact;
    }
}
