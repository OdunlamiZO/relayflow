package com.relayflow.api.workflow.engine;

import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;

public record ConversationMessageReceivedEvent(Conversation conversation, Message message) {}
