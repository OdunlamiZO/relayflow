package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#aiagentconfigurationscontroller

import io.odunlamizo.relayflow.agent.dto.AiAgentConfigurationResponse;
import io.odunlamizo.relayflow.agent.dto.LlmProviderResponse;
import io.odunlamizo.relayflow.agent.dto.PlatformLlmProviderResponse;
import io.odunlamizo.relayflow.agent.dto.UpdateAiAgentConfigurationRequest;
import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
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

    private final LlmClientFactory llmClientFactory;

    private final PluginRegistry pluginRegistry;

    public AiAgentConfigurationsController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService,
            LlmClientFactory llmClientFactory,
            PluginRegistry pluginRegistry) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
        this.llmClientFactory = llmClientFactory;
        this.pluginRegistry = pluginRegistry;
    }

    @GetMapping
    List<AiAgentConfigurationResponse> listConfigurations(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return aiAgentConfigurationService.listConfigurations(workspaceId);
    }

    @GetMapping("/platform-llm-provider")
    PlatformLlmProviderResponse getPlatformLlmProvider(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return new PlatformLlmProviderResponse(llmClientFactory.getActiveClient().provider());
    }

    @GetMapping("/llm-providers")
    List<LlmProviderResponse> listLlmProviders(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return pluginRegistry.chatModelProviders().stream()
                .map(provider -> new LlmProviderResponse(provider.id(), provider.displayName()))
                .toList();
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
