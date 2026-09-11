package com.relayflow.api.channel;

import com.relayflow.api.channel.dto.ChannelAccountResponse;
import com.relayflow.api.channel.dto.CreateChannelAccountRequest;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ChannelAccountController {

    private final ChannelAccountService channelAccountService;
    private final WorkspaceAuthorizationService authorizationService;

    public ChannelAccountController(
            ChannelAccountService channelAccountService,
            WorkspaceAuthorizationService authorizationService) {
        this.channelAccountService = channelAccountService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/channel-accounts")
    List<ChannelAccountResponse> listChannelAccounts(@RequestParam @NotNull UUID workspaceId) {
        return channelAccountService.listChannelAccounts(workspaceId);
    }

    @PostMapping("/channel-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    ChannelAccountResponse createChannelAccount(
            @Valid @RequestBody CreateChannelAccountRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                request.workspaceId(), authentication, WorkspacePermission.CHANNELS_WRITE);

        return channelAccountService.createChannelAccount(request);
    }

    @DeleteMapping("/channel-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disconnectChannelAccount(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CHANNELS_DELETE);

        channelAccountService.disconnectChannelAccount(id, workspaceId);
    }

    @PostMapping("/channel-accounts/{id}/reconnect")
    ChannelAccountResponse reconnectChannelAccount(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CHANNELS_WRITE);

        return channelAccountService.reconnectChannelAccount(id, workspaceId);
    }
}
