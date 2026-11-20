package io.odunlamizo.relayflow.agent.llm;

// Docs: docs/codebase-components.md#llmprompts

import io.odunlamizo.relayflow.agent.domain.ExtractionField;
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
                .append(" your knowledge base.")
                .append(
                        "\nescalate: set to true when the customer asks to talk to a person, or"
                                + " when you can't help them yourself. Your reply is sent first,"
                                + " so tell them a team member will reply here.")
                .append(
                        "\nneedsClarification: set to true when you can't tell what the customer"
                                + " wants and your reply asks them to explain.")
                .append(
                        "\n\nEach customer message may end with a <context>...</context> block —"
                                + " that's internal metadata about the contact, for you only. Never"
                                + " quote, repeat, or reference its literal text in your reply.");

        if (!extractionFields.isEmpty()) {
            sb.append(
                    "\n\n# DATA EXTRACTION\nPopulate \"extractedData\" with these keys whenever the"
                            + " information is present in the conversation — omit a key entirely if"
                            + " it isn't mentioned, don't guess:\n");
            for (ExtractionField field : extractionFields) {
                sb.append("\n- \"").append(field.key()).append("\": ").append(field.description());

                if (field.validationHook() != null && !field.validationHook().isBlank()) {
                    sb.append(
                            " (checked by the system — whenever the customer answers this, always"
                                    + " include it: convert it to the expected format when the"
                                    + " meaning is clear, otherwise copy exactly what they sent,"
                                    + " even if it looks wrong or incomplete)");
                }

                if (!field.askIfMissing()) {
                    sb.append(" (include it only if the customer mentions it — never ask for it)");
                }
            }
            sb.append(
                    "\n\nBefore finalizing your reply, check the customer's latest message against"
                            + " the \"Still missing\" fields inside the <context> block — if this"
                            + " message answers one of those fields, you must include it in"
                            + " extractedData, even if your reply already treats it as done. When"
                            + " you need to ask for a field, ask only for one listed as \"Still"
                            + " missing\", exactly one at a time, phrased plainly using its"
                            + " description above.");
        }

        return sb.toString();
    }

    private LlmPrompts() {}
}
