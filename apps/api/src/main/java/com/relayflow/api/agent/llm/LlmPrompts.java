package com.relayflow.api.agent.llm;

import com.relayflow.api.agent.domain.ExtractionField;
import java.util.List;

final class LlmPrompts {

    static String buildFormatInstruction(List<ExtractionField> extractionFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n# RESPONSE FORMAT — reply with this JSON only, no other text:\n")
                .append("{\"reply\":\"...\",\"confidence\":\"high\"|\"low\"|null,")
                .append("\"suggestedActions\":[],\"escalate\":false,\"needsClarification\":false")
                .append(extractionFields.isEmpty() ? "" : ",\"extractedData\":{}")
                .append("}\n")
                .append("suggestedActions values: \"mark_resolved\",")
                .append(" \"trigger_workflow:<workflowId>\"\n")
                .append("confidence: set to \"high\" only when you are certain your reply fully")
                .append(
                        " addresses the customer; use \"low\" when uncertain or the topic is outside")
                .append(" your knowledge base.");

        if (!extractionFields.isEmpty()) {
            sb.append(
                    "\n\n# DATA EXTRACTION\nPopulate \"extractedData\" with these keys whenever the"
                            + " information is present in the conversation — omit a key entirely if"
                            + " it isn't mentioned, don't guess:\n");
            for (ExtractionField field : extractionFields) {
                sb.append("\n- \"").append(field.key()).append("\": ").append(field.description());
            }
        }

        return sb.toString();
    }

    private LlmPrompts() {}
}
