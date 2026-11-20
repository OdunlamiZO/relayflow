package io.odunlamizo.relayflow.knowledge;

// Docs: docs/codebase-components.md#knowledgesourceservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.knowledge.KnowledgeSourceFetcher.FetchResult;
import io.odunlamizo.relayflow.knowledge.KnowledgeTemplateRenderer.Rendering;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSource;
import io.odunlamizo.relayflow.knowledge.domain.KnowledgeSourceHeader;
import io.odunlamizo.relayflow.knowledge.dto.KnowledgeSourceField;
import io.odunlamizo.relayflow.knowledge.dto.KnowledgeSourceFieldsResponse;
import io.odunlamizo.relayflow.knowledge.dto.KnowledgeSourceResponse;
import io.odunlamizo.relayflow.knowledge.dto.PreviewKnowledgeResponse;
import io.odunlamizo.relayflow.knowledge.dto.SaveKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceRequest;
import io.odunlamizo.relayflow.knowledge.dto.TestKnowledgeSourceResponse;
import io.odunlamizo.relayflow.knowledge.repository.KnowledgeSourceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeSourceService {

    static final int DEFAULT_CACHE_SECONDS = 60;

    static final int TEST_BODY_LIMIT = 20_000;

    static final int FIELD_LIMIT = 200;

    private static final int SAMPLE_LIMIT = 60;

    /** Keys a placeholder path can't reach, since these characters are part of its syntax. */
    private static final Pattern UNUSABLE_KEY = Pattern.compile("[.\\[\\]{}]");

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSourceService.class);

    private final KnowledgeSourceRepository sourceRepository;

    private final KnowledgeSourceFetcher fetcher;

    private final KnowledgeTemplateRenderer renderer;

    private final ObjectMapper objectMapper;

    public KnowledgeSourceService(
            KnowledgeSourceRepository sourceRepository,
            KnowledgeSourceFetcher fetcher,
            KnowledgeTemplateRenderer renderer,
            ObjectMapper objectMapper) {
        this.sourceRepository = sourceRepository;
        this.fetcher = fetcher;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSourceResponse> listSources(UUID workspaceId) {
        return sourceRepository.findByWorkspace(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public KnowledgeSourceResponse createSource(
            UUID workspaceId, SaveKnowledgeSourceRequest request) {
        KnowledgeSource source = new KnowledgeSource();
        source.setWorkspaceId(workspaceId);
        apply(source, request);

        save(source);

        log.info(
                "Knowledge source created {} {}",
                kv("workspaceId", workspaceId),
                kv("knowledgeSourceId", source.getId()));

        return toResponse(source);
    }

    @Transactional
    public KnowledgeSourceResponse updateSource(
            UUID workspaceId, UUID sourceId, SaveKnowledgeSourceRequest request) {
        KnowledgeSource source = getOrThrow(workspaceId, sourceId);

        if (!source.getName().equals(request.name().trim())) {
            assertUnused(workspaceId, source.getName(), "renamed");
        }

        apply(source, request);

        save(source);
        fetcher.evict(sourceId);

        log.info(
                "Knowledge source updated {} {}",
                kv("workspaceId", workspaceId),
                kv("knowledgeSourceId", sourceId));

        return toResponse(source);
    }

    @Transactional
    public void deleteSource(UUID workspaceId, UUID sourceId) {
        KnowledgeSource source = getOrThrow(workspaceId, sourceId);
        assertUnused(workspaceId, source.getName(), "deleted");

        sourceRepository.delete(source);
        fetcher.evict(sourceId);

        log.info(
                "Knowledge source deleted {} {}",
                kv("workspaceId", workspaceId),
                kv("knowledgeSourceId", sourceId));
    }

    @Transactional(readOnly = true)
    public TestKnowledgeSourceResponse testSource(
            UUID workspaceId, TestKnowledgeSourceRequest request) {
        FetchResult result =
                fetcher.fetchUncached(workspaceId, request.url(), request.headers()).join();

        return new TestKnowledgeSourceResponse(
                result.statusCode(), formatBody(result), result.error());
    }

    @Transactional(readOnly = true)
    public KnowledgeSourceFieldsResponse listFields(UUID workspaceId, UUID sourceId) {
        FetchResult result = fetcher.fetch(getOrThrow(workspaceId, sourceId)).join();

        if (result.failed()) {
            return new KnowledgeSourceFieldsResponse(List.of(), result.error());
        }

        List<KnowledgeSourceField> fields = new ArrayList<>();
        collectFields(result.json(), "", fields);

        return new KnowledgeSourceFieldsResponse(fields, null);
    }

    public PreviewKnowledgeResponse preview(UUID workspaceId, String text) {
        Rendering rendering = renderer.renderText(workspaceId, text);

        return new PreviewKnowledgeResponse(rendering.texts().getFirst(), rendering.failures());
    }

    private void assertUnused(UUID workspaceId, String name, String action) {
        List<String> agents = sourceRepository.findAiAgentNamesReferencing(workspaceId, name);

        if (!agents.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This source can't be "
                            + action
                            + " while the knowledge base of "
                            + String.join(", ", agents)
                            + " uses it. Remove it from there first.");
        }
    }

    /** Lists value paths, using only the first item of each array so long lists stay short. */
    private void collectFields(JsonNode node, String path, List<KnowledgeSourceField> fields) {
        if (fields.size() >= FIELD_LIMIT || node == null || node.isNull()) {
            return;
        }

        if (node.isValueNode()) {
            if (!path.isEmpty()) {
                fields.add(new KnowledgeSourceField(path, sample(node.asText())));
            }

            return;
        }

        if (node.isArray()) {
            if (!node.isEmpty()) {
                collectFields(node.get(0), path + "[0]", fields);
            }

            return;
        }

        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            String key = entry.getKey();

            if (key.isBlank() || UNUSABLE_KEY.matcher(key).find()) {
                continue;
            }

            collectFields(entry.getValue(), path.isEmpty() ? key : path + "." + key, fields);
        }
    }

    private String sample(String value) {
        return value.length() <= SAMPLE_LIMIT ? value : value.substring(0, SAMPLE_LIMIT) + "…";
    }

    private String formatBody(FetchResult result) {
        String body = result.body();

        if (result.json() != null) {
            try {
                body =
                        objectMapper
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(result.json());
            } catch (Exception exception) {
                body = result.body();
            }
        }

        if (body == null || body.length() <= TEST_BODY_LIMIT) {
            return body;
        }

        return body.substring(0, TEST_BODY_LIMIT) + "\n…";
    }

    private void apply(KnowledgeSource source, SaveKnowledgeSourceRequest request) {
        source.setName(request.name().trim());
        source.setUrl(request.url().trim());
        source.setHeaders(
                request.headers() != null
                        ? new ArrayList<>(
                                request.headers().stream()
                                        .map(
                                                header ->
                                                        new KnowledgeSourceHeader(
                                                                header.key().trim(),
                                                                header.value() != null
                                                                        ? header.value()
                                                                        : ""))
                                        .toList())
                        : new ArrayList<>());
        source.setCacheSeconds(
                request.cacheSeconds() != null ? request.cacheSeconds() : DEFAULT_CACHE_SECONDS);
    }

    private void save(KnowledgeSource source) {
        try {
            sourceRepository.saveAndFlush(source);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A knowledge source named \"" + source.getName() + "\" already exists");
        }
    }

    private KnowledgeSource getOrThrow(UUID workspaceId, UUID sourceId) {
        return sourceRepository
                .findInWorkspace(sourceId, workspaceId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Knowledge source not found"));
    }

    private KnowledgeSourceResponse toResponse(KnowledgeSource source) {
        return new KnowledgeSourceResponse(
                source.getId(),
                source.getName(),
                source.getUrl(),
                source.getHeaders(),
                source.getCacheSeconds(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
