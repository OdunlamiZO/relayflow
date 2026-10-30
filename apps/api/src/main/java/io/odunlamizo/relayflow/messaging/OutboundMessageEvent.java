package io.odunlamizo.relayflow.messaging;

// Docs: docs/codebase-components.md#outboundmessageevent

import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.messaging.domain.Message;
import java.util.List;

public record OutboundMessageEvent(
        Message message, ChannelAccount channelAccount, List<String> buttonOptions) {

    public OutboundMessageEvent(Message message, ChannelAccount channelAccount) {
        this(message, channelAccount, List.of());
    }
}
