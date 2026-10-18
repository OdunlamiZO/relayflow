package com.relayflow.api.workflow.engine;

import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;

public record ConversationOpenedEvent(Conversation conversation, Message triggeringMessage) {}
