package io.odunlamizo.relayflow.audio;

// Docs: docs/codebase-components.md#channelmediadownloader

import io.odunlamizo.relayflow.channel.domain.ChannelAccount;
import io.odunlamizo.relayflow.channel.domain.ChannelProvider;

public interface ChannelMediaDownloader {

    ChannelProvider provider();

    byte[] download(ChannelAccount channelAccount, String mediaReference);
}
