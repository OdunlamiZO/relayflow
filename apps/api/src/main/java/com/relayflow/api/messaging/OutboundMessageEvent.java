package com.relayflow.api.messaging;

import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.Message;

/**
 * Published after an outbound message is persisted. Channel adapters (e.g. Telegram) listen to this
 * to relay the message to the external platform.
 */
public record OutboundMessageEvent(Message message, ChannelAccount channelAccount) {}
