package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;

import io.odunlamizo.relayflow.agent.domain.AiAgentConfiguration;
import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import io.odunlamizo.relayflow.knowledge.repository.KnowledgeSourceRepository;
import io.odunlamizo.relayflow.support.ApplicationIntegrationTest;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class KnowledgeSourceRepositoryIT extends ApplicationIntegrationTest {

    @Autowired private KnowledgeSourceRepository sourceRepository;

    @Test
    void savesHeadersAndFindsSourcesByName() {
        UUID workspaceId = workspace();
        source(workspaceId, "rates");
        source(workspaceId, "fees");
        source(workspace(), "rates");
        flushAndClear();

        List<KnowledgeSource> found =
                sourceRepository.findByWorkspaceAndNames(workspaceId, List.of("rates", "stock"));

        assertThat(found).extracting(KnowledgeSource::getName).containsExactly("rates");
        assertThat(found.getFirst().getHeaders())
                .containsExactly(new KnowledgeSourceHeader("X-Key", "{{secrets.KEY}}"));
    }

    @Test
    void findsTheAgentsWhoseKnowledgeUsesASource() {
        UUID workspaceId = workspace();
        agent(workspaceId, "Sales", "Today's rate is {{sources.rates.usd}}");
        agent(workspaceId, "Support", "First branch: {{sources.rates[0].city}}");
        agent(workspaceId, "Billing", "Fees are {{sources.rates_v2.fee}}");
        flushAndClear();

        assertThat(sourceRepository.findAiAgentNamesReferencing(workspaceId, "rates"))
                .containsExactly("Sales", "Support");
    }

    private void source(UUID workspaceId, String name) {
        KnowledgeSource source = new KnowledgeSource();
        source.setWorkspaceId(workspaceId);
        source.setName(name);
        source.setUrl("https://example.com/" + name);
        source.setHeaders(List.of(new KnowledgeSourceHeader("X-Key", "{{secrets.KEY}}")));
        sourceRepository.save(source);
    }

    private void agent(UUID workspaceId, String name, String answer) {
        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setWorkspace(entityManager.getReference(Workspace.class, workspaceId));
        configuration.setName(name);
        configuration.setKnowledgeBase(List.of(new KnowledgeEntry("Question", answer)));
        entityManager.persist(configuration);
    }
}
