package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.relayflow.api.agent.domain.ExtractionField;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentWorkflowContextTest {

    @Test
    void buildsReplyConfidenceAndNamespacedExtractedDataVariables() {
        Map<String, String> context =
                AgentWorkflowContext.build(
                        "Sure, I can help.",
                        "high",
                        Map.of("orderNumber", "12345"),
                        List.of(new ExtractionField("orderNumber", "")));

        assertThat(context)
                .containsEntry("agent.reply", "Sure, I can help.")
                .containsEntry("agent.confidence", "high")
                .containsEntry("agent.data.orderNumber", "12345");
    }

    @Test
    void omitsConfidenceWhenNull() {
        Map<String, String> context =
                AgentWorkflowContext.build("Sure.", null, Map.of(), List.of());

        assertThat(context).doesNotContainKey("agent.confidence");
    }

    @Test
    void defaultsReplyToEmptyStringWhenNull() {
        Map<String, String> context = AgentWorkflowContext.build(null, null, Map.of(), List.of());

        assertThat(context).containsEntry("agent.reply", "");
    }

    @Test
    void dropsAnExtractedKeyThatIsNotAConfiguredExtractionField() {
        Map<String, String> context =
                AgentWorkflowContext.build(
                        "Sure.",
                        null,
                        Map.of("orderNumber", "12345", "hallucinated", "value"),
                        List.of(new ExtractionField("orderNumber", "")));

        assertThat(context)
                .containsEntry("agent.data.orderNumber", "12345")
                .doesNotContainKey("agent.data.hallucinated");
    }
}
