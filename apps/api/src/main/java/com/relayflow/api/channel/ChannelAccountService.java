package com.relayflow.api.channel;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.domain.ChannelAccountStatus;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.channel.dto.ChannelAccountResponse;
import com.relayflow.api.channel.dto.CreateChannelAccountRequest;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.security.CredentialEncryptionService;
import com.relayflow.api.telegram.TelegramWebhookRegistrar;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** Looks up a channel account within a workspace, or throws if not found. */
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

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
