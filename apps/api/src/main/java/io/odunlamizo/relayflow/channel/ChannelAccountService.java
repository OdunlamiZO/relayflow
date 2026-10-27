package io.odunlamizo.relayflow.channel;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelAccountStatus;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;
import io.odunlamizo.relayflow.channel.dto.ChannelAccountResponse;
import io.odunlamizo.relayflow.channel.dto.CreateChannelAccountRequest;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.common.MapUtils;
import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.security.CredentialEncryptionService;
import io.odunlamizo.relayflow.telegram.TelegramWebhookRegistrar;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelAccountService {

    private static final Logger log = LoggerFactory.getLogger(ChannelAccountService.class);

    private final ChannelAccountRepository channelAccountRepository;

    private final ChannelAccountMapper mapper;

    private final WorkspaceService workspaceService;

    private final TelegramWebhookRegistrar telegramWebhookRegistrar;

    private final CredentialEncryptionService credentialEncryptionService;

    public ChannelAccountService(
            ChannelAccountRepository channelAccountRepository,
            ChannelAccountMapper mapper,
            WorkspaceService workspaceService,
            TelegramWebhookRegistrar telegramWebhookRegistrar,
            CredentialEncryptionService credentialEncryptionService) {
        this.channelAccountRepository = channelAccountRepository;
        this.mapper = mapper;
        this.workspaceService = workspaceService;
        this.telegramWebhookRegistrar = telegramWebhookRegistrar;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Transactional(readOnly = true)
    public List<ChannelAccountResponse> listChannelAccounts(UUID workspaceId) {
        workspaceService.getWorkspace(workspaceId);

        return channelAccountRepository.findByWorkspace(workspaceId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ChannelAccountResponse createChannelAccount(CreateChannelAccountRequest request) {
        Workspace workspace = workspaceService.getWorkspace(request.workspaceId());

        // Keeps the plaintext token for webhook registration; it is encrypted before persisting.
        String plainTextCredential = request.encryptedCredentials();

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setWorkspace(workspace);
        channelAccount.setProvider(request.provider());
        channelAccount.setName(request.name());
        channelAccount.setStatus(
                request.status() == null ? ChannelAccountStatus.ACTIVE : request.status());
        channelAccount.setEncryptedCredentials(
                credentialEncryptionService.encrypt(plainTextCredential));
        channelAccount.setMetadata(MapUtils.copyMap(request.metadata()));

        channelAccountRepository.save(channelAccount);

        if (channelAccount.getProvider() == ChannelProvider.TELEGRAM
                && plainTextCredential != null) {
            String webhookSecret =
                    telegramWebhookRegistrar.register(plainTextCredential, channelAccount.getId());
            channelAccount.setWebhookSecret(webhookSecret);
            channelAccountRepository.save(channelAccount);
        }

        log.info(
                "Channel account created {} {} {}",
                kv("workspaceId", workspace.getId()),
                kv("channelAccountId", channelAccount.getId()),
                kv("provider", channelAccount.getProvider()));

        return mapper.toDto(channelAccount);
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
                "Channel account disconnected {} {} {}",
                kv("workspaceId", workspaceId),
                kv("channelAccountId", id),
                kv("provider", channelAccount.getProvider()));
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
                "Channel account reconnected {} {} {}",
                kv("workspaceId", workspaceId),
                kv("channelAccountId", id),
                kv("provider", channelAccount.getProvider()));

        return mapper.toDto(channelAccount);
    }

    @Transactional
    public void deleteChannelAccount(UUID id, UUID workspaceId) {
        ChannelAccount channelAccount =
                channelAccountRepository
                        .findInWorkspace(id, workspaceId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Channel account not found"));

        channelAccountRepository.delete(channelAccount);

        log.info(
                "Channel account deleted {} {} {}",
                kv("workspaceId", workspaceId),
                kv("channelAccountId", id),
                kv("provider", channelAccount.getProvider()));
    }

    public ChannelAccount getChannelAccount(UUID channelAccountId, UUID workspaceId) {
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
}
