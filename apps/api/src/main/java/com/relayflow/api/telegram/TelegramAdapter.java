package com.relayflow.api.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.messaging.OutboundMessageEvent;
import com.relayflow.api.messaging.ResourceNotFoundException;
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
import com.relayflow.api.messaging.repository.ChannelAccountRepository;
import com.relayflow.api.messaging.repository.ContactRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.telegram.dto.TelegramMessage;
import com.relayflow.api.telegram.dto.TelegramUser;
import com.relayflow.api.telegram.dto.TelegramWebhookPayload;
import com.relayflow.api.webhook.WebhookDispatchService;
import com.relayflow.api.webhook.WebhookEventType;
import com.relayflow.api.workflow.engine.ConversationMessageReceivedEvent;
import com.relayflow.api.workflow.engine.ConversationOpenedEvent;
import java.time.Instant;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TelegramAdapter {

    private static final Logger log = LoggerFactory.getLogger(TelegramAdapter.class);

    private static final String SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";

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

    public TelegramAdapter(
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

    @Transactional
    public void handleWebhook(
            UUID channelAccountId, String secretToken, TelegramWebhookPayload payload) {
        if (payload.message() == null || payload.message().text() == null) {
            return;
        }

        ChannelAccount channelAccount =
                channelAccountRepository
                        .findById(channelAccountId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        String expectedSecret = channelAccount.getWebhookSecret();
        if (expectedSecret != null && !expectedSecret.equals(secretToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid webhook secret");
        }

        if (channelAccount.getProvider() != ChannelProvider.TELEGRAM) {
            log.warn(
                    "Channel account {} is not a Telegram account — ignoring update",
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

        processInboundMessage(channelAccount, payload.message());
    }

    @EventListener
    public void onOutboundMessage(OutboundMessageEvent event) {
        ChannelAccount channelAccount = event.channelAccount();

        if (channelAccount.getProvider() != ChannelProvider.TELEGRAM) {
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
            throw new TelegramSendException(
                    "Telegram channel is disconnected — reconnect it from the channel settings",
                    null);
        }

        String botToken =
                credentialEncryptionService.decrypt(channelAccount.getEncryptedCredentials());

        if (botToken == null || botToken.isBlank()) {
            log.warn(
                    "No bot token configured for Telegram channel account {} — skipping relay",
                    channelAccount.getId());

            return;
        }

        Contact contact = message.getConversation().getContact();

        ExternalIdentity identity =
                externalIdentityRepository
                        .findForContact(channelAccount.getId(), contact.getId())
                        .orElseThrow(
                                () ->
                                        new TelegramSendException(
                                                "No Telegram identity for contact "
                                                        + contact.getId(),
                                                null));

        sendTelegramMessage(
                botToken,
                identity.getExternalConversationId(),
                message.getText(),
                event.buttonOptions());
    }

    /**
     * Core inbound-message processing. Runs within the caller's transaction — must not be called
     * via {@code this} from a {@code @Transactional} method (would bypass the proxy). {@link
     * #handleWebhook} is itself an entry point through the proxy, so the transaction is always
     * active.
     */
    private void processInboundMessage(ChannelAccount channelAccount, TelegramMessage msg) {
        TelegramUser from = msg.from();
        String externalUserId = String.valueOf(from.id());
        String chatId = String.valueOf(msg.chat().id());
        Workspace workspace = channelAccount.getWorkspace();
        UUID workspaceId = workspace.getId();

        ExternalIdentity identity =
                externalIdentityRepository
                        .findForExternalUser(channelAccount.getId(), externalUserId)
                        .orElseGet(
                                () -> createIdentity(channelAccount, from, externalUserId, chatId));

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

        // Three cases:
        //   1. No prior conversation → create one (triggersWorkflow = true)
        //   2. Existing OPEN conversation → use it as-is (triggersWorkflow = false)
        //   3. Existing CLOSED conversation → reopen it (triggersWorkflow = true, triggers
        // workflows)
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
        inboundMessage.setText(msg.text());
        inboundMessage.setProviderMessageId(String.valueOf(msg.messageId()));
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
                "Inbound message saved — workspace={} conversation={} from={}",
                workspaceId,
                conversation.getId(),
                contact.getDisplayName());

        if (triggersWorkflow) {
            eventPublisher.publishEvent(new ConversationOpenedEvent(conversation, inboundMessage));
        } else {
            // Existing open conversation — notify any waiting workflow runs to resume.
            eventPublisher.publishEvent(
                    new ConversationMessageReceivedEvent(conversation, inboundMessage));
        }

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "message.created",
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversation.getId().toString())));
    }

    // ── Telegram Bot API ─────────────────────────────────────────────────────

    /**
     * Sends a message to the given Telegram chat. When {@code buttons} is non-empty, attaches a
     * one-time reply keyboard so the user can tap an option instead of typing. Retries once on
     * failure.
     *
     * <p>Throws {@link TelegramSendException} if all attempts fail so the caller's transaction can
     * be rolled back and the HTTP client receives a proper error. Callers that want best-effort
     * fire-and-forget behavior (bot greetings, error hints) should use {@link
     * #sendTelegramMessageQuietly}.
     */
    private void sendTelegramMessage(
            String botToken, String chatId, String text, List<String> buttons) {
        if (chatId == null || chatId.isBlank()) {
            throw new TelegramSendException("No chat ID available for Telegram send", null);
        }

        String url = String.format(SEND_MESSAGE_URL, botToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");

        if (buttons != null && !buttons.isEmpty()) {
            // Each button row contains a single button object so options stack vertically.
            List<List<Map<String, String>>> keyboard =
                    buttons.stream().map(label -> List.of(Map.of("text", label))).toList();

            body.put(
                    "reply_markup",
                    Map.of(
                            "keyboard", keyboard,
                            "one_time_keyboard", true,
                            "resize_keyboard", true));
        }

        Exception lastEx = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                restTemplate.postForObject(url, body, String.class);

                return;
            } catch (Exception e) {
                lastEx = e;
                log.warn(
                        "Telegram send attempt {}/2 to chat {} failed: {}",
                        attempt,
                        chatId,
                        e.getMessage());
            }
        }

        throw new TelegramSendException(
                "Message failed to send — could not reach Telegram after 2 attempts", lastEx);
    }

    /**
     * Best-effort send — swallows {@link TelegramSendException} and logs at WARN. Used for
     * informational bot replies (connection confirmations, error hints) where a delivery failure
     * must not affect the caller's transaction or HTTP response.
     */
    private void sendTelegramMessageQuietly(String botToken, String chatId, String text) {
        try {
            sendTelegramMessage(botToken, chatId, text, List.of());
        } catch (TelegramSendException e) {
            log.warn(
                    "Bot reply to chat {} could not be delivered (non-fatal): {}",
                    chatId,
                    e.getMessage());
        }
    }

    private ExternalIdentity createIdentity(
            ChannelAccount channelAccount,
            TelegramUser from,
            String externalUserId,
            String chatId) {
        Workspace workspace = channelAccount.getWorkspace();

        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(buildDisplayName(from));
        contactRepository.save(contact);

        ExternalIdentity identity = new ExternalIdentity();
        identity.setWorkspace(workspace);
        identity.setContact(contact);
        identity.setChannelAccount(channelAccount);
        identity.setProvider(ChannelProvider.TELEGRAM);
        identity.setExternalUserId(externalUserId);
        identity.setExternalConversationId(chatId);
        identity.setUsername(from.username());
        identity.setRawProfile(new LinkedHashMap<>());

        identity = externalIdentityRepository.save(identity);

        Map<String, Object> contactData = new LinkedHashMap<>();
        contactData.put("id", contact.getId().toString());
        contactData.put(
                "displayName", contact.getDisplayName() != null ? contact.getDisplayName() : "");
        if (from.username() != null) {
            contactData.put("username", from.username());
        }

        Map<String, Object> channelData = new LinkedHashMap<>();
        channelData.put("id", channelAccount.getId().toString());
        channelData.put("name", channelAccount.getName());
        channelData.put("provider", ChannelProvider.TELEGRAM);

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

    private String buildDisplayName(TelegramUser from) {
        String firstName = from.firstName() == null ? "" : from.firstName().trim();
        String lastName = from.lastName() == null ? "" : from.lastName().trim();
        String full = (firstName + " " + lastName).trim();

        if (!full.isEmpty()) {
            return full;
        }

        return from.username() != null ? "@" + from.username() : "Telegram User";
    }
}
