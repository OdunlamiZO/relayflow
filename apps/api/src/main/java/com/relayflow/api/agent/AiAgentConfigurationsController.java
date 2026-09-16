package com.relayflow.api.agent;

import com.relayflow.api.agent.dto.AiAgentConfigurationResponse;
import com.relayflow.api.agent.dto.UpdateAiAgentConfigurationRequest;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}/ai-agent-configs")
public class AiAgentConfigurationsController {

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final WorkspaceAuthorizationService authorizationService;

    public AiAgentConfigurationsController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    List<AiAgentConfigurationResponse> listConfigurations(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return aiAgentConfigurationService.listConfigurations(workspaceId);
    }

    @PostMapping
    AiAgentConfigurationResponse createConfiguration(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateAiAgentConfigurationRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return aiAgentConfigurationService.createConfiguration(workspaceId, request);
    }
}
