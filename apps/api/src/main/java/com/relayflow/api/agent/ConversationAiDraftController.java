package com.relayflow.api.agent;

import com.relayflow.api.agent.dto.ConversationAiDraftResponse;
import com.relayflow.api.messaging.WorkspaceAuthorizationService;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.dto.MessageResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspaces/{workspaceId}/conversations/{conversationId}/ai-draft")
public class ConversationAiDraftController {

    private final AiAgentConfigurationService aiAgentConfigurationService;

    private final WorkspaceAuthorizationService authorizationService;

    public ConversationAiDraftController(
            AiAgentConfigurationService aiAgentConfigurationService,
            WorkspaceAuthorizationService authorizationService) {
        this.aiAgentConfigurationService = aiAgentConfigurationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    ConversationAiDraftResponse getDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return aiAgentConfigurationService.getDraft(workspaceId, conversationId);
    }

    @PostMapping("/send")
    MessageResponse sendDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        return aiAgentConfigurationService.sendDraft(workspaceId, conversationId);
    }

    @PostMapping("/trigger-workflow/{workflowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void triggerWorkflowFromDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            @PathVariable UUID workflowId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        aiAgentConfigurationService.triggerWorkflowFromDraft(
                workspaceId, conversationId, workflowId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void discardDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.INBOX);

        aiAgentConfigurationService.discardDraft(workspaceId, conversationId);
    }
}
