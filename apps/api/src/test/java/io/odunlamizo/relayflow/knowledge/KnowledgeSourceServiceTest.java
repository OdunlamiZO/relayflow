package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.knowledge.KnowledgeSourceFetcher.FetchResult;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import io.odunlamizo.relayflow.knowledge.dto.SaveKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceResponse;
import io.odunlamizo.relayflow.knowledge.repository.KnowledgeSourceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class KnowledgeSourceServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private KnowledgeSourceRepository sourceRepository;

    @Mock private KnowledgeSourceFetcher fetcher;

    @Mock private KnowledgeTemplateRenderer renderer;

    @Test
    void createTrimsTheFieldsAndDefaultsTheCacheTime() {
        service()
                .createSource(
                        WORKSPACE_ID,
                        new SaveKnowledgeSourceRequest(
                                " rates ",
                                " https://example.com/rates ",
                                List.of(new KnowledgeSourceHeader(" X-Key ", null)),
                                null));

        verify(sourceRepository)
                .saveAndFlush(
                        argThat(
                                source ->
                                        source.getName().equals("rates")
                                                && source.getUrl()
                                                        .equals("https://example.com/rates")
                                                && source.getHeaders()
                                                        .equals(
                                                                List.of(
                                                                        new KnowledgeSourceHeader(
                                                                                "X-Key", "")))
                                                && source.getCacheSeconds() == 60));
    }

    @Test
    void deleteRefusesASourceAnAgentStillUses() {
        KnowledgeSource source = source();
        when(sourceRepository.findAiAgentNamesReferencing(WORKSPACE_ID, "rates"))
                .thenReturn(List.of("Support agent"));

        assertThatThrownBy(() -> service().deleteSource(WORKSPACE_ID, source.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Support agent");

        verify(sourceRepository, never()).delete(any());
    }

    @Test
    void renameRefusesASourceAnAgentStillUses() {
        KnowledgeSource source = source();
        when(sourceRepository.findAiAgentNamesReferencing(WORKSPACE_ID, "rates"))
                .thenReturn(List.of("Support agent"));

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateSource(
                                                WORKSPACE_ID,
                                                source.getId(),
                                                new SaveKnowledgeSourceRequest(
                                                        "exchange_rates",
                                                        "https://example.com/rates",
                                                        List.of(),
                                                        60)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("renamed");
    }

    @Test
    void updateForgetsTheCachedResponse() {
        KnowledgeSource source = source();

        service()
                .updateSource(
                        WORKSPACE_ID,
                        source.getId(),
                        new SaveKnowledgeSourceRequest(
                                "rates", "https://example.com/v2/rates", List.of(), 30));

        verify(fetcher).evict(source.getId());
        assertThat(source.getUrl()).isEqualTo("https://example.com/v2/rates");
        assertThat(source.getCacheSeconds()).isEqualTo(30);
    }

    @Test
    void testFormatsTheJsonResponse() throws Exception {
        when(fetcher.fetchUncached(WORKSPACE_ID, "https://example.com/rates", List.of()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new FetchResult(
                                        200,
                                        "{\"usd\":1}",
                                        objectMapper.readTree("{\"usd\":1}"),
                                        null)));

        TestKnowledgeSourceResponse response =
                service()
                        .testSource(
                                WORKSPACE_ID,
                                new TestKnowledgeSourceRequest(
                                        "https://example.com/rates", List.of()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"usd\" : 1");
        assertThat(response.error()).isNull();
    }

    private KnowledgeSourceService service() {
        return new KnowledgeSourceService(sourceRepository, fetcher, renderer, objectMapper);
    }

    private KnowledgeSource source() {
        KnowledgeSource source = new KnowledgeSource();
        source.setId(UUID.randomUUID());
        source.setWorkspaceId(WORKSPACE_ID);
        source.setName("rates");
        source.setUrl("https://example.com/rates");
        when(sourceRepository.findInWorkspace(source.getId(), WORKSPACE_ID))
                .thenReturn(Optional.of(source));

        return source;
    }
}
