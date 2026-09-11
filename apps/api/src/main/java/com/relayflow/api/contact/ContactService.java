package com.relayflow.api.contact;

import com.relayflow.api.channel.ChannelAccountService;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.common.MapUtils;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.common.dto.PageResponse;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.domain.ExternalIdentity;
import com.relayflow.api.contact.dto.ContactDetailResponse;
import com.relayflow.api.contact.dto.ContactResponse;
import com.relayflow.api.contact.dto.CreateContactRequest;
import com.relayflow.api.contact.dto.CreateExternalIdentityRequest;
import com.relayflow.api.contact.dto.ExternalIdentityResponse;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.webhook.ContactSnapshotBuilder;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ContactService(
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            ContactMapper mapper,
            WorkspaceService workspaceService,
            ChannelAccountService channelAccountService,
            ReservedContactFieldResolver reservedContactFieldResolver,
            WebhookDispatchService webhookDispatchService) {
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.mapper = mapper;
        this.workspaceService = workspaceService;
        this.channelAccountService = channelAccountService;
        this.reservedContactFieldResolver = reservedContactFieldResolver;
        this.webhookDispatchService = webhookDispatchService;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspaceId());

        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(request.displayName());

        ContactResponse base = mapper.toDto(contactRepository.save(contact));

        return new ContactResponse(
                base.id(),
                base.workspaceId(),
                base.displayName(),
                base.customFields(),
                base.createdAt(),
                List.of());
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
                                c -> {
                                    ContactResponse base = mapper.toDto(c);
                                    List<ExternalIdentityResponse> ids =
                                            identitiesByContact.getOrDefault(c.getId(), List.of());

                                    return new ContactResponse(
                                            base.id(),
                                            base.workspaceId(),
                                            base.displayName(),
                                            base.customFields(),
                                            base.createdAt(),
                                            ids);
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

        // Derived values (e.g. WhatsApp phone) seed the map; explicitly stored values overwrite
        // them.
        Map<String, String> customFields =
                new LinkedHashMap<>(
                        reservedContactFieldResolver.resolve(contact, externalIdentities));
        customFields.putAll(contact.getCustomFields());

        return new ContactDetailResponse(
                contact.getId(),
                workspaceId,
                contact.getDisplayName(),
                customFields,
                contact.getCreatedAt(),
                identities);
    }

    @Transactional
    public ContactDetailResponse updateContactCustomFields(
            UUID contactId, UUID workspaceId, Map<String, String> customFields) {
        Contact contact = getContact(contactId, workspaceId);
        contact.setCustomFields(customFields);
        contactRepository.save(contact);

        webhookDispatchService.dispatch(
                workspaceId,
                WebhookEventType.CONTACT_UPDATED,
                ContactSnapshotBuilder.build(contact));

        return getContactDetail(contactId, workspaceId);
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

        // Guard: reject if both contacts have an identity on the same channel account —
        // that would mean two different external users on the same bot are being claimed
        // as the same person, which is never valid.
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
                        .collect(Collectors.groupingBy(ExternalIdentityResponse::contactId));

        List<ExternalIdentityResponse> mergedIdentities =
                identitiesByContact.getOrDefault(targetId, List.of());
        ContactResponse base = mapper.toDto(target);

        return new ContactResponse(
                base.id(),
                base.workspaceId(),
                base.displayName(),
                base.customFields(),
                base.createdAt(),
                mergedIdentities);
    }

    /** Looks up a contact within a workspace, or throws if not found. Shared by other services. */
    public Contact getContact(UUID contactId, UUID workspaceId) {
        Contact contact =
                contactRepository
                        .findById(contactId)
                        .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Contact not found");
        }

        return contact;
    }
}
