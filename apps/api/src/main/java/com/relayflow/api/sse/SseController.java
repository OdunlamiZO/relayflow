package com.relayflow.api.sse;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.workspace.WorkspaceService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Provides a Server-Sent Events stream for real-time workspace updates.
 *
 * <p>Clients connect to {@code GET /sse/workspace/{workspaceId}} and receive events as they are
 * broadcast by {@link WorkspaceSseService}. The caller must be a member of the workspace; guest
 * (anonymous) sessions are also accepted — membership is checked via {@link
 * WorkspaceService#listWorkspaces}.
 */
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
    public SseEmitter subscribe(@PathVariable UUID workspaceId, Authentication authentication) {
        UUID userId = securityUtils.resolveUserId(authentication);

        boolean isMember =
                workspaceService.listWorkspaces(userId).stream()
                        .anyMatch(ws -> ws.id().equals(workspaceId));

        if (!isMember) {
            throw new AccessDeniedException("Not a member of workspace " + workspaceId);
        }

        return sseService.subscribe(workspaceId);
    }
}
