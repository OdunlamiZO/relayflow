package com.relayflow.api.workflow.engine.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.api.workflow.engine.ExecutionContext;
import com.relayflow.api.workflow.engine.GraphNode;
import com.relayflow.api.workflow.engine.NodeExecutionException;
import com.relayflow.api.workflow.engine.NodeExecutionResult;
import com.relayflow.api.workspace.SecretService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpRequestNodeExecutorTest {

    @Mock private SecretService secretService;

    private HttpServer server;

    private final AtomicReference<String> receivedAuthHeader = new AtomicReference<>();

    private HttpRequestNodeExecutor executor() {
        return new HttpRequestNodeExecutor(new ObjectMapper(), secretService);
    }

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] body = "{}".getBytes();
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ExecutionContext context(UUID workspaceId) {
        return new ExecutionContext(UUID.randomUUID(), UUID.randomUUID(), workspaceId, Map.of());
    }

    @Test
    void resolvesASecretIntoAHeaderWithoutLeakingItIntoOutput() {
        UUID workspaceId = UUID.randomUUID();
        when(secretService.resolveDecrypted(workspaceId, "API_KEY"))
                .thenReturn(Optional.of("sk-super-secret"));

        NodeExecutionResult result =
                executor()
                        .execute(
                                new GraphNode(
                                        "n1",
                                        "httpRequest",
                                        Map.of(
                                                "method",
                                                "GET",
                                                "url",
                                                "http://127.0.0.1:" + server.getAddress().getPort(),
                                                "headers",
                                                java.util.List.of(
                                                        Map.of(
                                                                "key",
                                                                "Authorization",
                                                                "value",
                                                                "Bearer {{secrets.API_KEY}}")))),
                                context(workspaceId));

        assertThat(receivedAuthHeader.get()).isEqualTo("Bearer sk-super-secret");
        assertThat(result.output().toString()).doesNotContain("sk-super-secret");
    }

    @Test
    void failsTheStepWhenTheReferencedSecretDoesNotExist() {
        UUID workspaceId = UUID.randomUUID();
        when(secretService.resolveDecrypted(workspaceId, "MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                executor()
                                        .execute(
                                                new GraphNode(
                                                        "n1",
                                                        "httpRequest",
                                                        Map.of(
                                                                "method",
                                                                "GET",
                                                                "url",
                                                                "http://127.0.0.1:"
                                                                        + server.getAddress()
                                                                                .getPort(),
                                                                "headers",
                                                                java.util.List.of(
                                                                        Map.of(
                                                                                "key",
                                                                                "X-Key",
                                                                                "value",
                                                                                "{{secrets.MISSING}}")))),
                                                context(workspaceId)))
                .isInstanceOf(NodeExecutionException.class)
                .hasMessageContaining("MISSING");
    }
}
