package com.relayflow.api.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.ContactAccess;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SseController.class)
@AutoConfigureMockMvc(addFilters = false)
class SseControllerTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockBean private WorkspaceSseService sseService;

    @MockBean private WorkspaceService workspaceService;

    @MockBean private SecurityUtils securityUtils;

    @Test
    void streamIsMarkedSoProxiesNeitherCompressNorBufferIt() throws Exception {
        UUID userId = UUID.randomUUID();
        when(securityUtils.resolveUserId(any())).thenReturn(userId);
        when(workspaceService.listWorkspaces(userId))
                .thenReturn(
                        List.of(
                                new WorkspaceResponse(
                                        WORKSPACE_ID,
                                        "Acme",
                                        List.of(),
                                        List.of(),
                                        ContactAccess.ALL,
                                        null,
                                        Instant.now())));
        SseEmitter emitter = new SseEmitter();
        when(sseService.subscribe(WORKSPACE_ID)).thenReturn(emitter);

        MvcResult result =
                mockMvc.perform(get("/sse/workspace/{workspaceId}", WORKSPACE_ID))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        emitter.send(SseEmitter.event().data("ping"));

        assertThat(result.getResponse().getHeader("Cache-Control"))
                .isEqualTo("no-cache, no-transform");
        assertThat(result.getResponse().getHeader("X-Accel-Buffering")).isEqualTo("no");
    }
}
