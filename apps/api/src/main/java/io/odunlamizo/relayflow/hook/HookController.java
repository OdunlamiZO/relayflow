package io.odunlamizo.relayflow.hook;

import io.odunlamizo.relayflow.hook.dto.BuiltInHookResponse;
import io.odunlamizo.relayflow.hook.dto.HookResponse;
import io.odunlamizo.relayflow.hook.dto.SaveHookRequest;
import io.odunlamizo.relayflow.hook.dto.TestHookRequest;
import io.odunlamizo.relayflow.hook.dto.TestHookResponse;
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
@RequestMapping("/workspaces/{workspaceId}/hooks")
public class HookController {

    private final HookService hookService;

    private final WorkspaceAuthorizationService authorizationService;

    public HookController(
            HookService hookService, WorkspaceAuthorizationService authorizationService) {
        this.hookService = hookService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    List<HookResponse> listHooks(@PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return hookService.listHooks(workspaceId);
    }

    @GetMapping("/built-in")
    List<BuiltInHookResponse> listBuiltInHooks(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return hookService.listBuiltInHooks();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HookResponse createHook(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SaveHookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_WRITE);

        return hookService.createHook(workspaceId, request);
    }

    @PutMapping("/{hookId}")
    HookResponse updateHook(
            @PathVariable UUID workspaceId,
            @PathVariable UUID hookId,
            @Valid @RequestBody SaveHookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_WRITE);

        return hookService.updateHook(workspaceId, hookId, request);
    }

    @DeleteMapping("/{hookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteHook(
            @PathVariable UUID workspaceId,
            @PathVariable UUID hookId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_WRITE);

        hookService.deleteHook(workspaceId, hookId);
    }

    @PostMapping("/test")
    TestHookResponse testHook(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody TestHookRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.WORKFLOWS_WRITE);

        return hookService.testHook(request);
    }
}
