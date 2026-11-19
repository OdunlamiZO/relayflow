package io.odunlamizo.relayflow.providersettings;

// Docs: docs/codebase-components.md#providersettingscontroller

import io.odunlamizo.relayflow.providersettings.dto.ProviderSettingsResponse;
import io.odunlamizo.relayflow.providersettings.dto.UpdateProviderSettingsRequest;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
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
@RequestMapping("/workspaces/{workspaceId}/provider-settings")
public class ProviderSettingsController {

    private final ProviderSettingsService providerSettingsService;

    private final WorkspaceService workspaceService;

    private final WorkspaceAuthorizationService authorizationService;

    public ProviderSettingsController(
            ProviderSettingsService providerSettingsService,
            WorkspaceService workspaceService,
            WorkspaceAuthorizationService authorizationService) {
        this.providerSettingsService = providerSettingsService;
        this.workspaceService = workspaceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    ProviderSettingsResponse getSettings(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertMember(workspaceId, authentication);

        return providerSettingsService.getSettings(workspaceService.getWorkspace(workspaceId));
    }

    @PutMapping
    ProviderSettingsResponse updateSettings(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateProviderSettingsRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return providerSettingsService.updateSettings(
                workspaceService.getWorkspace(workspaceId), request);
    }
}
