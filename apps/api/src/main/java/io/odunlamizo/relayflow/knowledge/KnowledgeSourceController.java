package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#knowledgesourcecontroller

import io.odunlamizo.relayflow.knowledge.dto.KnowledgeSourceResponse;
import io.odunlamizo.relayflow.knowledge.dto.PreviewKnowledgeRequest;
import io.odunlamizo.relayflow.knowledge.dto.PreviewKnowledgeResponse;
import io.odunlamizo.relayflow.knowledge.dto.SaveKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceResponse;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}/knowledge-sources")
public class KnowledgeSourceController {

    private final KnowledgeSourceService sourceService;

    private final WorkspaceAuthorizationService authorizationService;

    public KnowledgeSourceController(
            KnowledgeSourceService sourceService,
            WorkspaceAuthorizationService authorizationService) {
        this.sourceService = sourceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    List<KnowledgeSourceResponse> listSources(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return sourceService.listSources(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    KnowledgeSourceResponse createSource(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveKnowledgeSourceRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return sourceService.createSource(workspaceId, request);
    }

    @PutMapping("/{sourceId}")
    KnowledgeSourceResponse updateSource(
            @PathVariable UUID workspaceId,
            @PathVariable UUID sourceId,
            @Valid @RequestBody SaveKnowledgeSourceRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return sourceService.updateSource(workspaceId, sourceId, request);
    }

    @DeleteMapping("/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSource(
            @PathVariable UUID workspaceId,
            @PathVariable UUID sourceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        sourceService.deleteSource(workspaceId, sourceId);
    }

    @PostMapping("/test")
    TestKnowledgeSourceResponse testSource(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody TestKnowledgeSourceRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return sourceService.testSource(workspaceId, request);
    }

    @PostMapping("/preview")
    PreviewKnowledgeResponse preview(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody PreviewKnowledgeRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.AI_AGENT_WRITE);

        return sourceService.preview(workspaceId, request.text());
    }
}
