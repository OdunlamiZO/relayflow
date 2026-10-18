package com.relayflow.api.messaging;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.Message;
import java.util.List;

public record OutboundMessageEvent(
        Message message, ChannelAccount channelAccount, List<String> buttonOptions) {

    public OutboundMessageEvent(Message message, ChannelAccount channelAccount) {
        this(message, channelAccount, List.of());
    }
}
