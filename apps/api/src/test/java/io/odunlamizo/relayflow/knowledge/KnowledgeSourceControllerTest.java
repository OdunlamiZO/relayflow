package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#backend-tests

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.authentication.SecurityUtils;
import io.odunlamizo.relayflow.knowledge.dto.PreviewKnowledgeRequest;
import io.odunlamizo.relayflow.knowledge.dto.PreviewKnowledgeResponse;
import io.odunlamizo.relayflow.knowledge.dto.SaveKnowledgeSourceRequest;
import io.odunlamizo.relayflow.workspace.WorkspaceAuthorizationService;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import java.util.List;
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

@WebMvcTest(KnowledgeSourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class KnowledgeSourceControllerTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private KnowledgeSourceService sourceService;

    @MockBean private WorkspaceAuthorizationService authorizationService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void previewReturnsTheFilledInText() throws Exception {
        when(sourceService.preview(WORKSPACE_ID, "{{sources.rates.usd}}"))
                .thenReturn(new PreviewKnowledgeResponse("1500", List.of()));

        mockMvc.perform(
                        post("/workspaces/{workspaceId}/knowledge-sources/preview", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new PreviewKnowledgeRequest(
                                                        "{{sources.rates.usd}}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("1500"));

        verify(authorizationService)
                .assertPermission(eq(WORKSPACE_ID), any(), eq(WorkspacePermission.AI_AGENT_WRITE));
    }

    @Test
    void createRejectsANameThatCannotBeUsedInAPlaceholder() throws Exception {
        mockMvc.perform(
                        post("/workspaces/{workspaceId}/knowledge-sources", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SaveKnowledgeSourceRequest(
                                                        "exchange rates",
                                                        "https://example.com/rates",
                                                        List.of(),
                                                        60))))
                .andExpect(status().isBadRequest());

        verify(sourceService, never()).createSource(any(), any());
    }

    @Test
    void createWithoutPermissionIsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authorizationService)
                .assertPermission(eq(WORKSPACE_ID), any(), eq(WorkspacePermission.AI_AGENT_WRITE));

        mockMvc.perform(
                        post("/workspaces/{workspaceId}/knowledge-sources", WORKSPACE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SaveKnowledgeSourceRequest(
                                                        "rates",
                                                        "https://example.com/rates",
                                                        List.of(),
                                                        60))))
                .andExpect(status().isForbidden());

        verify(sourceService, never()).createSource(any(), any());
    }
}
