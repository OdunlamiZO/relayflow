package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#knowledgetemplaterenderer

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.JsonNode;
import io.odunlamizo.relayflow.agent.domain.KnowledgeEntry;
import io.odunlamizo.relayflow.knowledge.KnowledgeSourceFetcher.FetchResult;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.repository.KnowledgeSourceRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fills {@code {{sources.NAME.path}}} placeholders in knowledge text with live values. */
@Service
public class KnowledgeTemplateRenderer {

    public static final String UNAVAILABLE = "(unavailable)";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTemplateRenderer.class);

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*sources\\.([A-Za-z][A-Za-z0-9_]*)([.\\[][^{}]*?)?\\s*}}");

    private static final Pattern PATH_SEPARATOR = Pattern.compile("\\.|(?=\\[)");

    private final KnowledgeSourceRepository sourceRepository;

    private final KnowledgeSourceFetcher fetcher;

    public KnowledgeTemplateRenderer(
            KnowledgeSourceRepository sourceRepository, KnowledgeSourceFetcher fetcher) {
        this.sourceRepository = sourceRepository;
        this.fetcher = fetcher;
    }

    @Transactional(readOnly = true)
    public RenderedKnowledge renderKnowledgeBase(UUID workspaceId, List<KnowledgeEntry> entries) {
        List<String> texts = new ArrayList<>();

        for (KnowledgeEntry entry : entries) {
            texts.add(entry.question());
            texts.add(entry.answer());
        }

        Rendering rendering = render(workspaceId, texts);
        List<KnowledgeEntry> rendered = new ArrayList<>(entries.size());

        for (int index = 0; index < entries.size(); index++) {
            rendered.add(
                    new KnowledgeEntry(
                            rendering.texts().get(index * 2),
                            rendering.texts().get(index * 2 + 1)));
        }

        if (!rendering.failures().isEmpty()) {
            log.warn(
                    "Some knowledge values couldn't be fetched {} {}",
                    kv("workspaceId", workspaceId),
                    kv("failures", rendering.failures()));
        }

        return new RenderedKnowledge(rendered, rendering.failures());
    }

    @Transactional(readOnly = true)
    public Rendering renderText(UUID workspaceId, String text) {
        Rendering rendering = render(workspaceId, List.of(text));

        return new Rendering(List.copyOf(rendering.texts()), rendering.failures());
    }

    private Rendering render(UUID workspaceId, List<String> texts) {
        Set<String> names = new LinkedHashSet<>();

        for (String text : texts) {
            if (text == null) {
                continue;
            }

            Matcher matcher = PLACEHOLDER.matcher(text);

            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }

        if (names.isEmpty()) {
            return new Rendering(texts, List.of());
        }

        Map<String, CompletableFuture<FetchResult>> fetches = new LinkedHashMap<>();

        for (KnowledgeSource source :
                sourceRepository.findByWorkspaceAndNames(workspaceId, names)) {
            fetches.put(source.getName(), fetcher.fetch(source));
        }

        CompletableFuture.allOf(fetches.values().toArray(CompletableFuture[]::new)).join();

        Set<String> failures = new LinkedHashSet<>();
        List<String> rendered = new ArrayList<>(texts.size());

        for (String text : texts) {
            rendered.add(text == null ? null : fill(text, fetches, failures));
        }

        return new Rendering(rendered, List.copyOf(failures));
    }

    private String fill(
            String text,
            Map<String, CompletableFuture<FetchResult>> fetches,
            Set<String> failures) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String name = matcher.group(1);
            String path = matcher.group(2) != null ? matcher.group(2).trim() : "";
            String value = value(name, path, fetches.get(name), failures);

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String value(
            String name, String path, CompletableFuture<FetchResult> fetch, Set<String> failures) {
        if (fetch == null) {
            failures.add("There's no knowledge source named \"" + name + "\"");

            return UNAVAILABLE;
        }

        FetchResult result = fetch.join();

        if (result.failed()) {
            failures.add(name + ": " + result.error());

            return UNAVAILABLE;
        }

        JsonNode node = find(result.json(), path);

        if (node == null || node.isMissingNode() || node.isNull()) {
            failures.add(name + ": nothing at \"" + path.replaceFirst("^\\.", "") + "\"");

            return UNAVAILABLE;
        }

        return node.isValueNode() ? node.asText() : node.toString();
    }

    private JsonNode find(JsonNode root, String path) {
        JsonNode current = root;

        for (String segment : PATH_SEPARATOR.split(path.replaceFirst("^\\.", ""))) {
            if (segment.isEmpty()) {
                continue;
            }

            if (current == null || current.isMissingNode()) {
                return null;
            }

            if (segment.startsWith("[") && segment.endsWith("]")) {
                segment = segment.substring(1, segment.length() - 1).trim();
            }

            if (current.isArray()) {
                try {
                    current = current.path(Integer.parseInt(segment));
                } catch (NumberFormatException exception) {
                    return null;
                }
            } else {
                current = current.path(segment);
            }
        }

        return current;
    }

    public record Rendering(List<String> texts, List<String> failures) {}
}
