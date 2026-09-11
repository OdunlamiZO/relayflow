package com.relayflow.api.agent;

import com.relayflow.api.agent.domain.AiAgentInvocationLog;
import com.relayflow.api.agent.domain.AiAgentInvocationStatus;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the atomic slot-claim step for AI agent invocations.
 *
 * <p>Running in its own {@code REQUIRES_NEW} transaction means a constraint violation on {@code
 * uq_ai_invocation_conversation_active} only rolls back this bean's transaction — it does not taint
 * the caller's transaction, avoiding {@code UnexpectedRollbackException}.
 */
@Component
public class AiAgentInvocationSlotClaimer {

    private final AiAgentInvocationLogRepository invocationLogRepository;

    public AiAgentInvocationSlotClaimer(AiAgentInvocationLogRepository invocationLogRepository) {
        this.invocationLogRepository = invocationLogRepository;
    }

    /**
     * Attempts to claim the active-invocation slot for {@code conversation} by inserting a RUNNING
     * log row. Returns the saved log on success, or empty if the slot is already taken.
     *
     * <p>Must be called through the Spring proxy (i.e., via injection, not {@code this.}).
     */
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
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }
}
