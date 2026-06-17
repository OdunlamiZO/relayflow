package com.relayflow.api.agent;

import com.relayflow.api.agent.dto.AiAgentConfigurationResponse;
import com.relayflow.api.agent.dto.UpdateAiAgentConfigurationRequest;
import com.relayflow.api.messaging.WorkspaceAuthorizationService;
import com.relayflow.api.messaging.domain.WorkspacePermission;
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
@RequestMapping("/workspaces/{workspaceId}/ai-agent-config")
public class AiAgentConfigurationController {

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final WorkspaceAuthorizationService authorizationService;

    public AiAgentConfigurationController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    AiAgentConfigurationResponse getConfiguration(@PathVariable UUID workspaceId) {
        return aiAgentConfigurationService.getOrCreateConfiguration(workspaceId);
    }

    @PutMapping
    AiAgentConfigurationResponse updateConfiguration(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateAiAgentConfigurationRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return aiAgentConfigurationService.updateConfiguration(workspaceId, request);
    }
}
