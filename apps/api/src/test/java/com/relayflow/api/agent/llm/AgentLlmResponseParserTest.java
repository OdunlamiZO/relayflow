package com.relayflow.api.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentLlmResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesExtractedDataIntoAStringMap() {
        String json =
                """
                {"reply":"Sure, I can help.","confidence":"high","suggestedActions":[],
                 "escalate":false,"needsClarification":false,
                 "extractedData":{"orderNumber":"12345","urgency":"high"}}
                """;

        AgentLlmResponse response = AgentLlmResponseParser.parse(objectMapper, json);

        assertThat(response.extractedData())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("orderNumber", "12345", "urgency", "high"));
    }

    @Test
    void defaultsToAnEmptyMapWhenExtractedDataIsAbsent() {
        String json =
                """
                {"reply":"Sure, I can help.","confidence":"high","suggestedActions":[],
                 "escalate":false,"needsClarification":false}
                """;

        AgentLlmResponse response = AgentLlmResponseParser.parse(objectMapper, json);

        assertThat(response.extractedData()).isEmpty();
    }

    @Test
    void defaultsToAnEmptyMapWhenExtractedDataIsNotAnObject() {
        String json =
                """
                {"reply":"Sure, I can help.","confidence":"high","suggestedActions":[],
                 "escalate":false,"needsClarification":false,"extractedData":null}
                """;

        AgentLlmResponse response = AgentLlmResponseParser.parse(objectMapper, json);

        assertThat(response.extractedData()).isEmpty();
    }

    @Test
    void stripsMarkdownCodeFences() {
        String json =
                """
                ```json
                {"reply":"Hi","confidence":null,"suggestedActions":[],"escalate":false,
                 "needsClarification":false,"extractedData":{"topic":"billing"}}
                ```
                """;

        AgentLlmResponse response = AgentLlmResponseParser.parse(objectMapper, json);

        assertThat(response.reply()).isEqualTo("Hi");
        assertThat(response.extractedData()).containsEntry("topic", "billing");
    }

    @Test
    void returnsAnEmptyResponseOnMalformedJson() {
        AgentLlmResponse response = AgentLlmResponseParser.parse(objectMapper, "not json");

        assertThat(response.reply()).isEmpty();
        assertThat(response.extractedData()).isEmpty();
        assertThat(response.suggestedActions()).isEmpty();
    }
}
