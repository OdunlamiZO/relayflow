package com.relayflow.api.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.relayflow.api.agent.domain.ExtractionField;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmPromptsTest {

    @Test
    void omitsTheExtractedDataSectionWhenNoFieldsAreConfigured() {
        String instruction = LlmPrompts.buildFormatInstruction(List.of());

        assertThat(instruction).doesNotContain("extractedData");
        assertThat(instruction).doesNotContain("DATA EXTRACTION");
    }

    @Test
    void listsConfiguredFieldsByKeyAndDescription() {
        List<ExtractionField> fields =
                List.of(
                        new ExtractionField("orderNumber", "The customer's order number"),
                        new ExtractionField("urgency", "How urgent the request is"));

        String instruction = LlmPrompts.buildFormatInstruction(fields);

        assertThat(instruction).contains("\"extractedData\":{}");
        assertThat(instruction).contains("DATA EXTRACTION");
        assertThat(instruction).contains("\"orderNumber\": The customer's order number");
        assertThat(instruction).contains("\"urgency\": How urgent the request is");
    }
}
