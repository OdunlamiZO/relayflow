package io.odunlamizo.relayflow.sse;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import io.odunlamizo.relayflow.authentication.SecurityUtils;
import io.odunlamizo.relayflow.workspace.WorkspaceService;
import io.odunlamizo.relayflow.workspace.domain.ContactAccess;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WorkspaceSseServiceTest {

    @Test
    void newStreamTellsTheBrowserToReconnectQuickly() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(workspaceService.listWorkspaces(userId))
                .thenReturn(
                        List.of(
                                new WorkspaceResponse(
                                        workspaceId,
                                        "Acme",
                                        List.of(),
                                        List.of(),
                                        ContactAccess.ALL,
                                        null,
                                        Instant.now())));

        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new SseController(
                                        new WorkspaceSseService(), workspaceService, securityUtils))
                        .build();

        MvcResult result =
                mockMvc.perform(get("/sse/workspace/{workspaceId}", workspaceId))
                        .andExpect(request().asyncStarted())
                        .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("retry:1000");
    }
}
