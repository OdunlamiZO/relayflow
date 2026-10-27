package io.odunlamizo.relayflow.workflow.engine;

import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;

public record ConversationMessageReceivedEvent(Conversation conversation, Message message) {}
