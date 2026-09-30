package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.hook.HookOutcome;
import com.relayflow.api.hook.HookService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExtractedDataValidator {

    private static final Logger log = LoggerFactory.getLogger(ExtractedDataValidator.class);

    private final HookService hookService;

    public ExtractedDataValidator(HookService hookService) {
        this.hookService = hookService;
    }

    public ExtractionValidation validate(
            UUID workspaceId,
            Map<String, String> extractedData,
            List<ExtractionField> extractionFields) {
        Map<String, ExtractionField> fieldsByKey =
                extractionFields.stream()
                        .collect(
                                Collectors.toMap(
                                        ExtractionField::key,
                                        Function.identity(),
                                        (first, second) -> first));

        Map<String, String> acceptedData = new LinkedHashMap<>();
        List<RejectedExtraction> rejections = new ArrayList<>();

        for (Map.Entry<String, String> entry : extractedData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            ExtractionField field = fieldsByKey.get(key);

            if (field == null
                    || field.validationHook() == null
                    || field.validationHook().isBlank()
                    || value == null
                    || value.isBlank()) {
                acceptedData.put(key, value);

                continue;
            }

            HookOutcome outcome =
                    hookService.runHook(workspaceId, field.validationHook(), value, Map.of(), null);

            switch (outcome.status()) {
                case ACCEPTED ->
                        acceptedData.put(
                                key,
                                outcome.value() != null ? String.valueOf(outcome.value()) : value);
                case REJECTED ->
                        rejections.add(new RejectedExtraction(key, value, outcome.errorMessage()));
                default -> {
                    log.warn(
                            "Validation hook {} failed for extraction field {} in workspace={}: {}",
                            field.validationHook(),
                            key,
                            workspaceId,
                            outcome.errorMessage());
                    rejections.add(
                            new RejectedExtraction(
                                    key,
                                    value,
                                    "Sorry, I couldn't process your "
                                            + key.replace('_', ' ')
                                            + ". Could you send it again?"));
                }
            }
        }

        return new ExtractionValidation(acceptedData, rejections);
    }
}
