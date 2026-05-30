package com.relayflow.api.workflow.engine;

import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;

/**
 * Published (after commit) when an inbound message arrives in an <em>existing</em> conversation.
 *
 * <p>Used by {@link WorkflowResumeListener} to resume any workflow runs that are currently paused
 * at a "Wait for Reply" node in that conversation.
 */
public record ConversationMessageReceivedEvent(Conversation conversation, Message message) {}
