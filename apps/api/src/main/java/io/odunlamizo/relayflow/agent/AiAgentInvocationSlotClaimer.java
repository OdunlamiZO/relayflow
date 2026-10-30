package io.odunlamizo.relayflow.agent;

// Docs: docs/codebase-components.md#aiagentinvocationslotclaimer

import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationLog;
import io.odunlamizo.relayflow.agent.domain.AiAgentInvocationStatus;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Runs in REQUIRES_NEW so a constraint violation leaves the caller's transaction intact. */
@Component
public class AiAgentInvocationSlotClaimer {

    private final AiAgentInvocationLogRepository invocationLogRepository;

    public AiAgentInvocationSlotClaimer(AiAgentInvocationLogRepository invocationLogRepository) {
        this.invocationLogRepository = invocationLogRepository;
    }

    /** Must be called through the Spring proxy. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AiAgentInvocationLog> tryClaim(
            Conversation conversation, Message triggeringMessage) {
        AiAgentInvocationLog invocationLog = new AiAgentInvocationLog();
        invocationLog.setWorkspace(conversation.getWorkspace());
        invocationLog.setConversation(conversation);
        invocationLog.setStatus(AiAgentInvocationStatus.RUNNING);
        invocationLog.setInputSnapshot(
                Map.of(
                        "messageText",
                        triggeringMessage.getText() != null ? triggeringMessage.getText() : ""));

        try {
            return Optional.of(invocationLogRepository.saveAndFlush(invocationLog));
        } catch (DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }
}
