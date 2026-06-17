package com.relayflow.api.agent.llm;

final class LlmPrompts {

    static final String JSON_FORMAT_INSTRUCTION =
            "\n\n# RESPONSE FORMAT — reply with this JSON only, no other text:\n"
                    + "{\"reply\":\"...\",\"confidence\":\"high\"|\"low\"|null,"
                    + "\"suggestedActions\":[],\"escalate\":false,\"needsClarification\":false}\n"
                    + "suggestedActions values: \"mark_resolved\","
                    + " \"trigger_workflow:<workflowId>\"\n"
                    + "confidence: set to \"high\" only when you are certain your reply fully"
                    + " addresses the customer; use \"low\" when uncertain or the topic is outside"
                    + " your knowledge base.";

    private LlmPrompts() {}
}
