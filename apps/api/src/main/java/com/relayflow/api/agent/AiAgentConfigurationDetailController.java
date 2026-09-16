package com.relayflow.api.agent;

import com.relayflow.api.agent.dto.AiAgentConfigurationResponse;
import com.relayflow.api.agent.dto.UpdateAiAgentConfigurationRequest;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}/ai-agent-configs/{configurationId}")
public class AiAgentConfigurationDetailController {

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final WorkspaceAuthorizationService authorizationService;

    public AiAgentConfigurationDetailController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
    }

    @PutMapping
    AiAgentConfigurationResponse updateConfiguration(
            @PathVariable UUID workspaceId,
            @PathVariable UUID configurationId,
            @Valid @RequestBody UpdateAiAgentConfigurationRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return aiAgentConfigurationService.updateConfiguration(
                workspaceId, configurationId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteConfiguration(
            @PathVariable UUID workspaceId,
            @PathVariable UUID configurationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        aiAgentConfigurationService.deleteConfiguration(workspaceId, configurationId);
    }

    @PostMapping("/set-default")
    AiAgentConfigurationResponse setDefault(
            @PathVariable UUID workspaceId,
            @PathVariable UUID configurationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return aiAgentConfigurationService.setDefaultConfiguration(workspaceId, configurationId);
    }
}
