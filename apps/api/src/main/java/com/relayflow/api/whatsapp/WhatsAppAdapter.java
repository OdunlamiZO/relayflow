package com.relayflow.api.whatsapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.domain.ChannelAccountStatus;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.domain.ExternalIdentity;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ConversationStatus;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.sse.SseEventType;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.domain.WebhookEventType;
import com.relayflow.api.whatsapp.dto.WhatsAppContactEntry;
import com.relayflow.api.whatsapp.dto.WhatsAppMessage;
import com.relayflow.api.whatsapp.dto.WhatsAppWebhookPayload;
import com.relayflow.api.workflow.engine.ConversationMessageReceivedEvent;
import com.relayflow.api.workflow.engine.ConversationOpenedEvent;
import com.relayflow.api.workspace.domain.Workspace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WhatsAppAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapter.class);

    private static final String GRAPH_API_VERSION = "v21.0";

    private static final String SEND_MESSAGE_URL =
            "https://graph.facebook.com/" + GRAPH_API_VERSION + "/%s/messages";

    private final ChannelAccountRepository channelAccountRepository;

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final RestTemplate restTemplate;

    private final CredentialEncryptionService credentialEncryptionService;

    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper;

    private final WebhookDispatchService webhookDispatchService;

    public WhatsAppAdapter(
            ChannelAccountRepository channelAccountRepository,
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            RestTemplate restTemplate,
            CredentialEncryptionService credentialEncryptionService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            WebhookDispatchService webhookDispatchService) {
        this.channelAccountRepository = channelAccountRepository;
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.restTemplate = restTemplate;
        this.credentialEncryptionService = credentialEncryptionService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.webhookDispatchService = webhookDispatchService;
    }

    // ── Webhook verification ─────────────────────────────────────────────────

    /**
     * Handles the GET challenge Meta sends when you register (or update) a webhook URL in the
     * Developer Console. Meta sends:
     *
     * <pre>
     * GET /whatsapp/webhook/{id}?hub.mode=subscribe&amp;hub.verify_token=...&amp;hub.challenge=...
     * </pre>
     *
     * <p>We verify the token against the stored credential and, on match, echo back {@code
     * hub.challenge} as a plain-text response body.
     */
    public String verifyWebhook(
            UUID channelAccountId, String mode, String verifyToken, String challenge) {
        ChannelAccount channelAccount =
                channelAccountRepository
                        .findById(channelAccountId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        if (channelAccount.getProvider() != ChannelProvider.WHATSAPP) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel account not found");
        }

        WhatsAppCredentials credentials = decryptCredentials(channelAccount);

        if (!"subscribe".equals(mode) || !credentials.verifyToken().equals(verifyToken)) {
            log.warn(
                    "WhatsApp webhook verification failed for channel account {} — token mismatch or bad mode",
                    channelAccountId);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verification failed");
        }

        log.info("WhatsApp webhook verified for channel account {}", channelAccountId);

        return challenge;
    }

    @Transactional
    public void handleWebhook(UUID channelAccountId, WhatsAppWebhookPayload payload) {
        if (payload.entry() == null || payload.entry().isEmpty()) {
            return;
        }

        ChannelAccount channelAccount =
                channelAccountRepository
                        .findById(channelAccountId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        if (channelAccount.getProvider() != ChannelProvider.WHATSAPP) {
            log.warn(
                    "Channel account {} is not a WhatsApp account — ignoring update",
                    channelAccountId);

            return;
        }

        if (channelAccount.getStatus() != ChannelAccountStatus.ACTIVE) {
            log.debug(
                    "Channel account {} is not active (status={}) — ignoring update",
                    channelAccountId,
                    channelAccount.getStatus());

            return;
        }

        for (var entry : payload.entry()) {
            if (entry.changes() == null) {
                continue;
            }

            for (var change : entry.changes()) {
                // Only process message events; skip status updates (delivery receipts etc.).
                if (!"messages".equals(change.field()) || change.value() == null) {
                    continue;
                }

                var value = change.value();

                if (value.messages() == null || value.messages().isEmpty()) {
                    continue;
                }

                List<WhatsAppContactEntry> contacts =
                        value.contacts() != null ? value.contacts() : List.of();

                for (WhatsAppMessage msg : value.messages()) {
                    String msgText = resolveMessageText(msg);

                    if (msgText == null) {
                        continue;
                    }

                    String displayName = resolveDisplayName(msg.from(), contacts);

                    processInboundMessage(channelAccount, msg, msgText, displayName);
                }
            }
        }
    }

    @EventListener
    public void onOutboundMessage(OutboundMessageEvent event) {
        ChannelAccount channelAccount = event.channelAccount();

        if (channelAccount.getProvider() != ChannelProvider.WHATSAPP) {
            return;
        }

        Message message = event.message();

        if (message.getDirection() != MessageDirection.OUTBOUND) {
            return;
        }

        if (message.getText() == null || message.getText().isBlank()) {
            return;
        }

        if (channelAccount.getStatus() != ChannelAccountStatus.ACTIVE) {
            throw new WhatsAppSendException(
                    "WhatsApp channel is disconnected — reconnect it from the channel settings",
                    null);
        }

        WhatsAppCredentials credentials = decryptCredentials(channelAccount);

        if (credentials.accessToken() == null
                || credentials.accessToken().isBlank()
                || credentials.phoneNumberId() == null
                || credentials.phoneNumberId().isBlank()) {
            log.warn(
                    "Incomplete credentials for WhatsApp channel account {} — skipping relay",
                    channelAccount.getId());

            return;
        }

        Contact contact = message.getConversation().getContact();

        ExternalIdentity identity =
                externalIdentityRepository
                        .findForContact(channelAccount.getId(), contact.getId())
                        .orElseThrow(
                                () ->
                                        new WhatsAppSendException(
                                                "No WhatsApp identity for contact "
                                                        + contact.getId(),
                                                null));

        // externalConversationId holds the recipient's wa_id (phone number).
        sendWhatsAppMessage(
                credentials,
                identity.getExternalConversationId(),
                message.getText(),
                event.buttonOptions());
    }

    private void processInboundMessage(
            ChannelAccount channelAccount,
            WhatsAppMessage msg,
            String messageText,
            String displayName) {
        String waId = msg.from();
        Workspace workspace = channelAccount.getWorkspace();
        UUID workspaceId = workspace.getId();

        ExternalIdentity identity =
                externalIdentityRepository
                        .findForExternalUser(channelAccount.getId(), waId)
                        .orElseGet(() -> createIdentity(channelAccount, waId, displayName));

        Contact contact = identity.getContact();

        Optional<Conversation> latestConversation =
                conversationRepository
                        .findLatestConversationForContact(
                                workspaceId,
                                channelAccount.getId(),
                                contact.getId(),
                                PageRequest.of(0, 1))
                        .stream()
                        .findFirst();

        boolean triggersWorkflow;
        Conversation conversation;

        if (latestConversation.isEmpty()) {
            conversation = createConversation(workspace, contact, channelAccount);
            triggersWorkflow = true;
        } else if (latestConversation.get().getStatus() == ConversationStatus.CLOSED) {
            conversation = latestConversation.get();
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setSessionStartedAt(Instant.now());
            conversationRepository.save(conversation);
            triggersWorkflow = true;
        } else {
            conversation = latestConversation.get();
            triggersWorkflow = false;
        }

        Message inboundMessage = new Message();
        inboundMessage.setWorkspace(workspace);
        inboundMessage.setConversation(conversation);
        inboundMessage.setDirection(MessageDirection.INBOUND);
        inboundMessage.setSenderType(MessageSenderType.CONTACT);
        inboundMessage.setText(messageText);
        inboundMessage.setProviderMessageId(msg.id());
        inboundMessage.setRawPayload(
                objectMapper.convertValue(
                        msg, new TypeReference<LinkedHashMap<String, Object>>() {}));
        inboundMessage = messageRepository.save(inboundMessage);

        conversation.setLastMessageAt(
                inboundMessage.getCreatedAt() != null
                        ? inboundMessage.getCreatedAt()
                        : Instant.now());
        conversationRepository.save(conversation);

        log.info(
                "Inbound WhatsApp message saved — workspace={} conversation={} from={}",
                workspaceId,
                conversation.getId(),
                contact.getDisplayName());

        if (triggersWorkflow) {
            eventPublisher.publishEvent(new ConversationOpenedEvent(conversation, inboundMessage));
        } else {
            eventPublisher.publishEvent(
                    new ConversationMessageReceivedEvent(conversation, inboundMessage));
        }

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        SseEventType.MESSAGE_CREATED,
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversation.getId().toString())));
    }

    /**
     * Sends a message via the WhatsApp Cloud API. Retries once on failure.
     *
     * <ul>
     *   <li>1–3 button options → interactive button message (native tap targets).
     *   <li>4+ button options → plain text with numbered choices appended (API limit is 3 buttons).
     *   <li>No options → plain text message.
     * </ul>
     *
     * <p>Throws {@link WhatsAppSendException} if all attempts fail so the caller's transaction can
     * be rolled back and the HTTP client receives a proper error.
     */
    private void sendWhatsAppMessage(
            WhatsAppCredentials credentials,
            String recipientWaId,
            String text,
            List<String> buttonOptions) {
        if (recipientWaId == null || recipientWaId.isBlank()) {
            throw new WhatsAppSendException("No recipient wa_id for WhatsApp send", null);
        }

        String url = String.format(SEND_MESSAGE_URL, credentials.phoneNumberId());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = buildOutboundBody(recipientWaId, text, buttonOptions);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Exception lastEx = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                return;
            } catch (Exception e) {
                lastEx = e;
                log.warn(
                        "WhatsApp send attempt {}/2 to {} failed: {}",
                        attempt,
                        recipientWaId,
                        e.getMessage());
            }
        }

        throw new WhatsAppSendException(
                "Message failed to send — could not reach WhatsApp after 2 attempts", lastEx);
    }

    /**
     * Builds the JSON body for a WhatsApp Cloud API outbound message.
     *
     * <p>When {@code buttonOptions} has 1–3 items the message uses the {@code interactive/button}
     * type so recipients see native tap targets. For 4+ options, the options are appended as a
     * numbered list because the Cloud API caps buttons at 3.
     */
    private Map<String, Object> buildOutboundBody(
            String recipientWaId, String text, List<String> buttonOptions) {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("messaging_product", "whatsapp");
        base.put("recipient_type", "individual");
        base.put("to", recipientWaId);

        if (buttonOptions != null && !buttonOptions.isEmpty() && buttonOptions.size() <= 3) {
            // Interactive button message (max 3 buttons per WhatsApp Cloud API spec).
            List<Map<String, Object>> buttons = new ArrayList<>();

            for (int i = 0; i < buttonOptions.size(); i++) {
                Map<String, Object> reply = new LinkedHashMap<>();
                reply.put("id", "btn-" + i);
                // Button title is limited to 20 characters by the API.
                String title = buttonOptions.get(i);
                reply.put("title", title.length() > 20 ? title.substring(0, 20) : title);

                Map<String, Object> button = new LinkedHashMap<>();
                button.put("type", "reply");
                button.put("reply", reply);
                buttons.add(button);
            }

            Map<String, Object> action = Map.of("buttons", buttons);
            Map<String, Object> interactive = new LinkedHashMap<>();
            interactive.put("type", "button");
            interactive.put("body", Map.of("text", text));
            interactive.put("action", action);

            base.put("type", "interactive");
            base.put("interactive", interactive);

        } else if (buttonOptions != null && buttonOptions.size() > 3) {
            // Fallback: append numbered options as plain text.
            StringBuilder sb = new StringBuilder(text).append("\n\n");

            for (int i = 0; i < buttonOptions.size(); i++) {
                sb.append(i + 1).append(". ").append(buttonOptions.get(i)).append("\n");
            }

            base.put("type", "text");
            base.put("text", Map.of("preview_url", false, "body", sb.toString().trim()));

        } else {
            base.put("type", "text");
            base.put("text", Map.of("preview_url", false, "body", text));
        }

        return base;
    }

    private WhatsAppCredentials decryptCredentials(ChannelAccount channelAccount) {
        String json = credentialEncryptionService.decrypt(channelAccount.getEncryptedCredentials());

        try {
            return objectMapper.readValue(json, WhatsAppCredentials.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse WhatsApp credentials for channel account "
                            + channelAccount.getId(),
                    e);
        }
    }

    /**
     * Extracts the human-readable message text from an inbound WhatsApp message regardless of
     * whether it arrived as a plain {@code text} message or as an {@code interactive} button reply.
     * Returns {@code null} for unsupported message types so the caller can skip them.
     */
    private String resolveMessageText(WhatsAppMessage msg) {
        if ("text".equals(msg.type())) {
            return msg.text() != null ? msg.text().body() : null;
        }

        if ("interactive".equals(msg.type())
                && msg.interactive() != null
                && msg.interactive().buttonReply() != null) {
            return msg.interactive().buttonReply().title();
        }

        return null;
    }

    private String resolveDisplayName(String waId, List<WhatsAppContactEntry> contacts) {
        return contacts.stream()
                .filter(contact -> waId.equals(contact.waId()))
                .findFirst()
                .map(contact -> contact.profile() != null ? contact.profile().name() : null)
                .filter(name -> name != null && !name.isBlank())
                .orElse("+" + waId);
    }

    private ExternalIdentity createIdentity(
            ChannelAccount channelAccount, String waId, String displayName) {
        Workspace workspace = channelAccount.getWorkspace();

        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(displayName);
        contactRepository.save(contact);

        ExternalIdentity identity = new ExternalIdentity();
        identity.setWorkspace(workspace);
        identity.setContact(contact);
        identity.setChannelAccount(channelAccount);
        identity.setProvider(ChannelProvider.WHATSAPP);
        identity.setExternalUserId(waId);
        // WhatsApp is always 1:1; the conversation is identified by the contact's phone number.
        identity.setExternalConversationId(waId);
        identity.setUsername(null);
        identity.setRawProfile(new LinkedHashMap<>());

        identity = externalIdentityRepository.save(identity);

        Map<String, Object> contactData = new LinkedHashMap<>();
        contactData.put("id", contact.getId().toString());
        contactData.put(
                "displayName", contact.getDisplayName() != null ? contact.getDisplayName() : "");

        Map<String, Object> channelData = new LinkedHashMap<>();
        channelData.put("id", channelAccount.getId().toString());
        channelData.put("name", channelAccount.getName());
        channelData.put("provider", ChannelProvider.WHATSAPP);

        Map<String, Object> contactPayload = new LinkedHashMap<>();
        contactPayload.put("contact", contactData);
        contactPayload.put("channel", channelData);

        webhookDispatchService.dispatch(
                workspace.getId(), WebhookEventType.CONTACT_CREATED, contactPayload);

        return identity;
    }

    private Conversation createConversation(
            Workspace workspace, Contact contact, ChannelAccount channelAccount) {
        Conversation conversation = new Conversation();
        conversation.setWorkspace(workspace);
        conversation.setContact(contact);
        conversation.setChannelAccount(channelAccount);
        conversation.setStatus(ConversationStatus.OPEN);

        return conversationRepository.save(conversation);
    }
}
