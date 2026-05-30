package com.relayflow.api.workflow.engine;

import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;

/**
 * Published by MessagingService when a brand-new conversation is created by an inbound message.
 * WorkflowTriggerListener picks this up to fire any enabled workflows for the workspace.
 */
public record ConversationOpenedEvent(Conversation conversation, Message triggeringMessage) {}
