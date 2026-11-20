package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#knowledgetemplaterenderer

import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import java.util.List;

/** {@code failures} names each value that couldn't be filled in, and why. */
public record RenderedKnowledge(List<KnowledgeEntry> entries, List<String> failures) {

    public static RenderedKnowledge unchanged(List<KnowledgeEntry> entries) {
        return new RenderedKnowledge(entries, List.of());
    }
}
