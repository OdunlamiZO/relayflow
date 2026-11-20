package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import io.odunlamizo.relayflow.knowledge.KnowledgeSourceFetcher.FetchResult;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.repository.KnowledgeSourceRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeTemplateRendererTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private KnowledgeSourceRepository sourceRepository;

    @Mock private KnowledgeSourceFetcher fetcher;

    @Test
    void fillsValuesFromTheSourcesResponse() throws Exception {
        KnowledgeSource rates = source("rates");
        when(sourceRepository.findByWorkspaceAndNames(any(), any())).thenReturn(List.of(rates));
        when(fetcher.fetch(rates))
                .thenReturn(
                        success(
                                "{\"usd\":1520.5,\"updated\":\"today\","
                                        + "\"branches\":[{\"city\":\"Lagos\"}]}"));

        RenderedKnowledge rendered =
                renderer()
                        .renderKnowledgeBase(
                                WORKSPACE_ID,
                                List.of(
                                        new KnowledgeEntry(
                                                "What's the dollar rate?",
                                                "{{sources.rates.usd}} as of"
                                                        + " {{ sources.rates.updated }}, from"
                                                        + " {{sources.rates.branches[0].city}}")));

        assertThat(rendered.entries().getFirst().answer())
                .isEqualTo("1520.5 as of today, from Lagos");
        assertThat(rendered.failures()).isEmpty();
    }

    @Test
    void fetchesEachSourceOnceForManyPlaceholders() throws Exception {
        KnowledgeSource rates = source("rates");
        when(sourceRepository.findByWorkspaceAndNames(any(), any())).thenReturn(List.of(rates));
        when(fetcher.fetch(rates)).thenReturn(success("{\"usd\":1,\"gbp\":2}"));

        renderer()
                .renderKnowledgeBase(
                        WORKSPACE_ID,
                        List.of(
                                new KnowledgeEntry("USD?", "{{sources.rates.usd}}"),
                                new KnowledgeEntry("GBP?", "{{sources.rates.gbp}}")));

        verify(fetcher, times(1)).fetch(rates);
    }

    @Test
    void marksValuesUnavailableWhenTheFetchFails() {
        KnowledgeSource rates = source("rates");
        when(sourceRepository.findByWorkspaceAndNames(any(), any())).thenReturn(List.of(rates));
        when(fetcher.fetch(rates))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                FetchResult.failure(503, "down", "HTTP 503")));

        RenderedKnowledge rendered =
                renderer()
                        .renderKnowledgeBase(
                                WORKSPACE_ID,
                                List.of(new KnowledgeEntry("Rate?", "{{sources.rates.usd}}")));

        assertThat(rendered.entries().getFirst().answer())
                .isEqualTo(KnowledgeTemplateRenderer.UNAVAILABLE);
        assertThat(rendered.failures()).containsExactly("rates: HTTP 503");
    }

    @Test
    void marksMissingFieldsAndUnknownSourcesUnavailable() throws Exception {
        KnowledgeSource rates = source("rates");
        when(sourceRepository.findByWorkspaceAndNames(any(), any())).thenReturn(List.of(rates));
        when(fetcher.fetch(rates)).thenReturn(success("{\"usd\":null}"));

        RenderedKnowledge rendered =
                renderer()
                        .renderKnowledgeBase(
                                WORKSPACE_ID,
                                List.of(
                                        new KnowledgeEntry(
                                                "Rate?",
                                                "{{sources.rates.usd}} {{sources.fees.lagos}}")));

        assertThat(rendered.entries().getFirst().answer()).isEqualTo("(unavailable) (unavailable)");
        assertThat(rendered.failures())
                .containsExactly(
                        "rates: nothing at \"usd\"", "There's no knowledge source named \"fees\"");
    }

    @Test
    void leavesPlainKnowledgeAloneWithoutLookingUpSources() {
        List<KnowledgeEntry> entries = List.of(new KnowledgeEntry("Hours?", "9 to 5"));

        RenderedKnowledge rendered = renderer().renderKnowledgeBase(WORKSPACE_ID, entries);

        assertThat(rendered.entries()).isEqualTo(entries);
        verifyNoInteractions(sourceRepository, fetcher);
    }

    private KnowledgeTemplateRenderer renderer() {
        return new KnowledgeTemplateRenderer(sourceRepository, fetcher);
    }

    private KnowledgeSource source(String name) {
        KnowledgeSource source = new KnowledgeSource();
        source.setId(UUID.randomUUID());
        source.setWorkspaceId(WORKSPACE_ID);
        source.setName(name);

        return source;
    }

    private CompletableFuture<FetchResult> success(String json) throws Exception {
        return CompletableFuture.completedFuture(
                new FetchResult(200, json, objectMapper.readTree(json), null));
    }
}
