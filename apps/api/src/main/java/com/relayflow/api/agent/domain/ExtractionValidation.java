package com.relayflow.api.agent.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ExtractionValidation(
        Map<String, String> acceptedData, List<RejectedExtraction> rejections) {

    public boolean hasRejections() {
        return !rejections.isEmpty();
    }

    public String correctionInstruction(String draftReply) {
        String rejected =
                rejections.stream()
                        .map(
                                rejection ->
                                        "- "
                                                + rejection.key()
                                                + ": the customer sent \""
                                                + rejection.value()
                                                + "\" — "
                                                + rejection.reason())
                        .collect(Collectors.joining("\n"));

        return "\n\n# CORRECTION REQUIRED\n"
                + "Your draft reply was not sent, because values the customer gave failed"
                + " validation:\n"
                + rejected
                + "\n\nDraft reply: \""
                + draftReply
                + "\"\n\nWrite a new reply to the customer's last message that:\n"
                + "1. Tells them which value wasn't accepted and why, quoting what they sent.\n"
                + "2. Asks them to send it again, with an example of the expected format when"
                + " that helps.\n"
                + "3. Still answers anything else they asked in that message.\n"
                + "Never just repeat an earlier question without saying what was wrong, and don't"
                + " treat the rejected values as received.";
    }

    public String fallbackReply() {
        return rejections.stream()
                .map(RejectedExtraction::reason)
                .distinct()
                .collect(Collectors.joining("\n"));
    }
}
