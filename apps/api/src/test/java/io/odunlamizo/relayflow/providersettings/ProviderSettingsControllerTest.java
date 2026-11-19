package io.odunlamizo.relayflow.providersettings;

// Docs: docs/codebase-components.md#backend-tests

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.authentication.SecurityUtils;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ProviderSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProviderSettingsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProviderSettingsService providerSettingsService;

    @MockBean private WorkspaceService workspaceService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void onlyAnOwnerCanChangeTheProviders() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace owner required"))
                .when(authorizationService)
                .assertOwner(eq(workspaceId), any());

        mockMvc.perform(
                        put("/workspaces/{workspaceId}/provider-settings", workspaceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"llmProvider\":\"ANTHROPIC\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(providerSettingsService);
    }
}
