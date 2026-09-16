package com.relayflow.api.agent;

import com.relayflow.api.agent.dto.ChannelAiAgentAssignmentResponse;
import com.relayflow.api.agent.dto.SetChannelAiAgentConfigurationRequest;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}/channel-accounts/{channelAccountId}/ai-agent-config")
public class ChannelAiAgentConfigurationController {

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final WorkspaceAuthorizationService authorizationService;

    public ChannelAiAgentConfigurationController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    ChannelAiAgentAssignmentResponse getAssignment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelAccountId,
            Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return aiAgentConfigurationService.getChannelAssignment(workspaceId, channelAccountId);
    }

    @PutMapping
    ChannelAiAgentAssignmentResponse setAssignment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelAccountId,
            @Valid @RequestBody SetChannelAiAgentConfigurationRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return aiAgentConfigurationService.assignConfigurationToChannel(
                workspaceId, channelAccountId, request.configurationId());
    }
}
