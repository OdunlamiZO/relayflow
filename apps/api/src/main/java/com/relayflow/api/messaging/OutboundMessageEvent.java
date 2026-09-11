package com.relayflow.api.messaging;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.Message;
import java.util.List;

/**
 * Published after an outbound message is persisted. Channel adapters (e.g. Telegram, WhatsApp)
 * listen to this to relay the message to the external platform.
 *
 * <p>{@code buttonOptions} carries the list of option labels to present as interactive buttons /
 * keyboard shortcuts on supported channels. Empty for plain text messages.
 */
public record OutboundMessageEvent(
        Message message, ChannelAccount channelAccount, List<String> buttonOptions) {

    /** Convenience constructor for messages with no interactive button options. */
    public OutboundMessageEvent(Message message, ChannelAccount channelAccount) {
        this(message, channelAccount, List.of());
    }
}
