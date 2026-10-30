package io.odunlamizo.relayflow.sse;

// Docs: docs/codebase-components.md#ssecontroller

import io.odunlamizo.relayflow.authentication.SecurityUtils;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final WorkspaceSseService sseService;

    private final WorkspaceService workspaceService;

    private final SecurityUtils securityUtils;

    public SseController(
            WorkspaceSseService sseService,
            WorkspaceService workspaceService,
            SecurityUtils securityUtils) {
        this.sseService = sseService;
        this.workspaceService = workspaceService;
        this.securityUtils = securityUtils;
    }

    @GetMapping(value = "/workspace/{workspaceId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
            @PathVariable UUID workspaceId, Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        boolean isMember =
                workspaceService.listWorkspaces(userId).stream()
                        .anyMatch(workspace -> workspace.id().equals(workspaceId));

        if (!isMember) {
            throw new AccessDeniedException("Not a member of workspace " + workspaceId);
        }

        // Stops proxies in front of the API from compressing or buffering the stream.
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(sseService.subscribe(workspaceId));
    }
}
