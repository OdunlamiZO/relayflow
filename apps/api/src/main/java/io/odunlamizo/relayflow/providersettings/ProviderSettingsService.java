package io.odunlamizo.relayflow.providersettings;

// Docs: docs/codebase-components.md#providersettingsservice

import io.odunlamizo.relayflow.agent.llm.LlmClient;
import io.odunlamizo.relayflow.agent.llm.LlmClientFactory;
import io.odunlamizo.relayflow.agent.llm.LlmPlatformConfigService;
import io.odunlamizo.relayflow.audio.AudioProviderResolver;
import io.odunlamizo.relayflow.plugin.PluginProvider;
import io.odunlamizo.relayflow.pluginregistry.PluginRegistry;
import io.odunlamizo.relayflow.providersettings.dto.ProviderOptionResponse;
import io.odunlamizo.relayflow.providersettings.dto.ProviderSettingsResponse;
import io.odunlamizo.relayflow.providersettings.dto.ProviderTypeSettingsResponse;
import io.odunlamizo.relayflow.providersettings.dto.UpdateProviderSettingsRequest;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceProviderSettings;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProviderSettingsService {

    private final PluginRegistry pluginRegistry;

    private final LlmClientFactory llmClientFactory;

    private final LlmPlatformConfigService llmPlatformConfigService;

    private final AudioProviderResolver audioProviderResolver;

    private final WorkspaceRepository workspaceRepository;

    public ProviderSettingsService(
            PluginRegistry pluginRegistry,
            LlmClientFactory llmClientFactory,
            LlmPlatformConfigService llmPlatformConfigService,
            AudioProviderResolver audioProviderResolver,
            WorkspaceRepository workspaceRepository) {
        this.pluginRegistry = pluginRegistry;
        this.llmClientFactory = llmClientFactory;
        this.llmPlatformConfigService = llmPlatformConfigService;
        this.audioProviderResolver = audioProviderResolver;
        this.workspaceRepository = workspaceRepository;
    }

    public ProviderSettingsResponse getSettings(Workspace workspace) {
        WorkspaceProviderSettings settings = workspace.getProviderSettings();

        ProviderTypeSettingsResponse chatModel =
                new ProviderTypeSettingsResponse(
                        settings.llmProvider(),
                        llmClientFactory.instanceProvider().map(PluginProvider::id).orElse(null),
                        pluginRegistry.chatModelProviders().stream()
                                .map(
                                        provider ->
                                                option(
                                                        provider,
                                                        settings.llmModel(provider.id())
                                                                .orElse(null),
                                                        llmPlatformConfigService.getConfigValue(
                                                                LlmClient.modelKey(provider.id()),
                                                                null)))
                                .toList());

        ProviderTypeSettingsResponse transcription =
                new ProviderTypeSettingsResponse(
                        settings.transcriptionProvider(),
                        audioProviderResolver
                                .instanceTranscriptionProvider()
                                .map(PluginProvider::id)
                                .orElse(null),
                        pluginRegistry.transcriptionProviders().stream()
                                .map(
                                        provider ->
                                                option(
                                                        provider,
                                                        settings.transcriptionModel(provider.id())
                                                                .orElse(null),
                                                        audioProviderResolver
                                                                .instanceTranscriptionModel(
                                                                        provider.id())))
                                .toList());

        ProviderTypeSettingsResponse storage =
                new ProviderTypeSettingsResponse(
                        settings.storageProvider(),
                        audioProviderResolver
                                .instanceStorageProvider()
                                .map(PluginProvider::id)
                                .orElse(null),
                        pluginRegistry.storageProviders().stream()
                                .map(provider -> option(provider, null, null))
                                .toList());

        return new ProviderSettingsResponse(chatModel, transcription, storage);
    }

    @Transactional
    public ProviderSettingsResponse updateSettings(
            Workspace workspace, UpdateProviderSettingsRequest request) {
        WorkspaceProviderSettings current = workspace.getProviderSettings();

        workspace.setProviderSettings(
                new WorkspaceProviderSettings(
                        installedOrUnchanged(
                                request.llmProvider(),
                                current.llmProvider(),
                                pluginRegistry::findChatModelProvider,
                                "LLM"),
                        models(request.llmModels()),
                        installedOrUnchanged(
                                request.transcriptionProvider(),
                                current.transcriptionProvider(),
                                pluginRegistry::findTranscriptionProvider,
                                "transcription"),
                        models(request.transcriptionModels()),
                        installedOrUnchanged(
                                request.storageProvider(),
                                current.storageProvider(),
                                pluginRegistry::findStorageProvider,
                                "storage")));

        return getSettings(workspaceRepository.save(workspace));
    }

    private static ProviderOptionResponse option(
            PluginProvider provider, String model, String instanceModel) {
        return new ProviderOptionResponse(
                provider.id(),
                provider.displayName(),
                PluginRegistry.configurationProblem(provider).orElse(null),
                model,
                instanceModel);
    }

    private static String installedOrUnchanged(
            String requested,
            String current,
            Function<String, Optional<? extends PluginProvider>> finder,
            String type) {
        String providerId = requested != null && !requested.isBlank() ? requested.strip() : null;

        if (providerId == null
                || providerId.equals(current)
                || finder.apply(providerId).isPresent()) {
            return providerId;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The " + type + " plugin " + providerId + " isn't installed");
    }

    private static Map<String, String> models(Map<String, String> requested) {
        Map<String, String> models = new LinkedHashMap<>();

        if (requested == null) {
            return models;
        }

        requested.forEach(
                (providerId, model) -> {
                    if (providerId != null && model != null && !model.isBlank()) {
                        models.put(providerId.strip(), model.strip());
                    }
                });

        return models;
    }
}
