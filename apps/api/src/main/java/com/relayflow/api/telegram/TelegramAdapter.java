package com.relayflow.api.telegram;

import com.relayflow.api.configuration.CredentialEncryptionService;
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
import com.relayflow.api.sse.SseBroadcastEvent;
import com.relayflow.api.telegram.dto.TelegramMessage;
import com.relayflow.api.telegram.dto.TelegramUser;
import com.relayflow.api.telegram.dto.TelegramWebhookPayload;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

    private final String sharedBotToken;

    private final String webBaseUrl;

    public TelegramAdapter(
            ChannelAccountRepository channelAccountRepository,
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            RestTemplate restTemplate,
            CredentialEncryptionService credentialEncryptionService,
            ApplicationEventPublisher eventPublisher,
            @Value("${shared.telegram.bot-token:}") String sharedBotToken,
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl) {
        this.channelAccountRepository = channelAccountRepository;
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.restTemplate = restTemplate;
        this.credentialEncryptionService = credentialEncryptionService;
        this.eventPublisher = eventPublisher;
        this.sharedBotToken = sharedBotToken;
        this.webBaseUrl = webBaseUrl;
    }

    // ── Inbound ──────────────────────────────────────────────────────────────

    @Transactional
    public void handleWebhook(UUID channelAccountId, TelegramWebhookPayload payload) {
        if (payload.message() == null || payload.message().text() == null) {
            return;
        }

        ChannelAccount channelAccount =
                channelAccountRepository
                        .findById(channelAccountId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

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

    /**
     * Core inbound-message processing shared by both the per-workspace and shared-bot webhook
     * paths. Runs within the caller's transaction — must not be called via {@code this} from a
     * {@code @Transactional} method (would bypass the proxy). Both callers ({@link #handleWebhook}
     * and {@link #handleSharedBotMessage}) are themselves entry points through the proxy, so the
     * transaction is always active.
     */
    private void processInboundMessage(ChannelAccount channelAccount, TelegramMessage msg) {
        TelegramUser from = msg.from();
        String externalUserId = String.valueOf(from.id());
        String chatId = String.valueOf(msg.chat().id());
        Workspace workspace = channelAccount.getWorkspace();
        UUID workspaceId = workspace.getId();

        ExternalIdentity identity =
                externalIdentityRepository
                        .findForExternalUser(workspaceId, ChannelProvider.TELEGRAM, externalUserId)
                        .orElseGet(() -> createIdentity(workspace, from, externalUserId, chatId));

        Contact contact = identity.getContact();

        Conversation conversation =
                conversationRepository
                        .findOpenConversationForContact(
                                workspaceId, channelAccount.getId(), contact.getId())
                        .orElseGet(() -> createConversation(workspace, contact, channelAccount));

        Message inboundMessage = new Message();
        inboundMessage.setWorkspace(workspace);
        inboundMessage.setConversation(conversation);
        inboundMessage.setDirection(MessageDirection.INBOUND);
        inboundMessage.setSenderType(MessageSenderType.CONTACT);
        inboundMessage.setText(msg.text());
        inboundMessage.setProviderMessageId(String.valueOf(msg.messageId()));
        inboundMessage.setRawPayload(new LinkedHashMap<>());
        Message saved = messageRepository.save(inboundMessage);

        conversation.setLastMessageAt(
                saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now());
        conversationRepository.save(conversation);

        log.info(
                "Inbound message saved — workspace={} conversation={} from={}",
                workspaceId,
                conversation.getId(),
                contact.getDisplayName());

        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "message.created",
                        Map.of(
                                "workspaceId", workspaceId.toString(),
                                "conversationId", conversation.getId().toString())));
    }

    // ── Shared bot webhook ───────────────────────────────────────────────────

    /**
     * Handles updates received by the shared bot.
     *
     * <p>{@code /start {workspaceId}} — links the Telegram user to that guest workspace and replies
     * with the inbox URL. Subsequent messages from the same Telegram user are routed automatically
     * via their {@link com.relayflow.api.messaging.domain.ExternalIdentity}.
     */
    @Transactional
    public void handleSharedBotWebhook(TelegramWebhookPayload webhook) {
        if (webhook.message() == null || webhook.message().from() == null) {
            return;
        }

        var msg = webhook.message();
        String text = msg.text();
        String chatId = String.valueOf(msg.chat().id());
        String telegramUserId = String.valueOf(msg.from().id());

        if (text == null) {
            return;
        }

        if (text.startsWith("/start")) {
            String param = text.length() > 7 ? text.substring(7).trim() : "";
            handleSharedBotStart(param, msg.from(), telegramUserId, chatId);
        } else {
            handleSharedBotMessage(msg, telegramUserId, chatId);
        }
    }

    private void handleSharedBotStart(
            String workspaceIdParam, TelegramUser from, String telegramUserId, String chatId) {
        if (workspaceIdParam.isEmpty()) {
            sendTelegramMessageQuietly(
                    sharedBotToken,
                    chatId,
                    "To link your inbox, click 'Connect Telegram' inside your inbox first.");

            return;
        }

        UUID workspaceId;

        try {
            workspaceId = UUID.fromString(workspaceIdParam);
        } catch (IllegalArgumentException e) {
            sendTelegramMessageQuietly(sharedBotToken, chatId, "Invalid workspace link.");

            return;
        }

        List<ChannelAccount> accounts =
                channelAccountRepository.findAllByProvider(workspaceId, ChannelProvider.TELEGRAM);

        ChannelAccount channelAccount =
                accounts.stream()
                        .filter(
                                ca ->
                                        sharedBotToken.equals(
                                                credentialEncryptionService.decrypt(
                                                        ca.getEncryptedCredentials())))
                        .findFirst()
                        .orElse(null);

        if (channelAccount == null) {
            sendTelegramMessageQuietly(sharedBotToken, chatId, "Guest workspace not found.");

            return;
        }

        if (channelAccount.getStatus() != ChannelAccountStatus.ACTIVE) {
            sendTelegramMessageQuietly(
                    sharedBotToken, chatId, "This workspace is not currently accepting messages.");

            return;
        }

        Workspace workspace = channelAccount.getWorkspace();

        // Find or create the identity for this workspace. Bump createdAt so that
        // this workspace sorts as the most-recently linked one when routing messages
        // across multiple guest workspaces for the same Telegram user.
        ExternalIdentity identity =
                externalIdentityRepository
                        .findForExternalUser(workspaceId, ChannelProvider.TELEGRAM, telegramUserId)
                        .orElseGet(() -> createIdentity(workspace, from, telegramUserId, chatId));

        identity.setCreatedAt(Instant.now());
        externalIdentityRepository.save(identity);

        log.info("Guest Telegram user {} linked to workspace {}", telegramUserId, workspaceId);

        String inboxUrl = webBaseUrl + "/inbox?workspaceId=" + workspaceId;
        sendTelegramMessageQuietly(
                sharedBotToken,
                chatId,
                "Connected! ✅ Send me any message to see it appear in your inbox:\n" + inboxUrl);

        // Notify the inbox that the workspace state has changed (Telegram now linked).
        eventPublisher.publishEvent(
                new SseBroadcastEvent(
                        workspaceId,
                        "workspace.updated",
                        Map.of("workspaceId", workspaceId.toString())));
    }

    private void handleSharedBotMessage(TelegramMessage msg, String telegramUserId, String chatId) {
        if (msg.text() == null) {
            return;
        }

        // findAllForExternalUser returns identities newest-first (ORDER BY createdAt DESC).
        // The flatMap tries each workspace in that order, so the most-recently linked guest
        // workspace wins when the same Telegram user has connected across multiple sessions.
        List<ExternalIdentity> identities =
                externalIdentityRepository.findAllForExternalUser(
                        ChannelProvider.TELEGRAM, telegramUserId);

        ChannelAccount sharedChannelAccount =
                identities.stream()
                        .flatMap(
                                identity ->
                                        channelAccountRepository
                                                .findAllByProvider(
                                                        identity.getWorkspace().getId(),
                                                        ChannelProvider.TELEGRAM)
                                                .stream())
                        .filter(ca -> ca.getStatus() == ChannelAccountStatus.ACTIVE)
                        .filter(
                                ca ->
                                        sharedBotToken.equals(
                                                credentialEncryptionService.decrypt(
                                                        ca.getEncryptedCredentials())))
                        .findFirst()
                        .orElse(null);

        if (sharedChannelAccount == null) {
            sendTelegramMessageQuietly(
                    sharedBotToken,
                    chatId,
                    "To get started, visit relayflow.io and click 'Try it', then connect Telegram from your inbox.");

            return;
        }

        processInboundMessage(sharedChannelAccount, msg);
    }

    // ── Outbound relay ───────────────────────────────────────────────────────

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
                        .findForContact(
                                channelAccount.getWorkspace().getId(),
                                ChannelProvider.TELEGRAM,
                                contact.getId())
                        .orElseThrow(
                                () ->
                                        new TelegramSendException(
                                                "No Telegram identity for contact "
                                                        + contact.getId(),
                                                null));

        sendTelegramMessage(botToken, identity.getExternalConversationId(), message.getText());
    }

    // ── Telegram Bot API ─────────────────────────────────────────────────────

    /**
     * Sends a message to the given Telegram chat. Retries once on failure.
     *
     * <p>Throws {@link TelegramSendException} if all attempts fail so the caller's transaction can
     * be rolled back and the HTTP client receives a proper error. Callers that want best-effort
     * fire-and-forget behaviour (bot greetings, error hints) should use {@link
     * #sendTelegramMessageQuietly}.
     */
    private void sendTelegramMessage(String botToken, String chatId, String text) {
        if (chatId == null || chatId.isBlank()) {
            throw new TelegramSendException("No chat ID available for Telegram send", null);
        }

        String url = String.format(SEND_MESSAGE_URL, botToken);
        Map<String, String> body = Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML");
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
            sendTelegramMessage(botToken, chatId, text);
        } catch (TelegramSendException e) {
            log.warn(
                    "Bot reply to chat {} could not be delivered (non-fatal): {}",
                    chatId,
                    e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ExternalIdentity createIdentity(
            Workspace workspace, TelegramUser from, String externalUserId, String chatId) {
        Contact contact = new Contact();
        contact.setWorkspace(workspace);
        contact.setDisplayName(buildDisplayName(from));
        contactRepository.save(contact);

        ExternalIdentity identity = new ExternalIdentity();
        identity.setWorkspace(workspace);
        identity.setContact(contact);
        identity.setProvider(ChannelProvider.TELEGRAM);
        identity.setExternalUserId(externalUserId);
        identity.setExternalConversationId(chatId);
        identity.setUsername(from.username());
        identity.setRawProfile(new LinkedHashMap<>());

        return externalIdentityRepository.save(identity);
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
