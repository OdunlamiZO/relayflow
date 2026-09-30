package com.relayflow.api.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChannelAccountMapperTest {

    private ChannelAccountMapper mapper(String apiBaseUrl) {
        ChannelAccountMapper mapper = new ChannelAccountMapperImpl();
        ReflectionTestUtils.setField(mapper, "apiBaseUrl", apiBaseUrl);

        return mapper;
    }

    private ChannelAccount channelAccount(ChannelProvider provider) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setId(UUID.randomUUID());
        channelAccount.setWorkspace(workspace);
        channelAccount.setProvider(provider);
        channelAccount.setName("My channel");

        return channelAccount;
    }

    @Test
    void buildsTelegramWebhookUrlFromThePublicApiAddress() {
        ChannelAccount channelAccount = channelAccount(ChannelProvider.TELEGRAM);

        assertThat(mapper("https://api.example.com/").toDto(channelAccount).webhookUrl())
                .isEqualTo("https://api.example.com/telegram/webhook/" + channelAccount.getId());
    }

    @Test
    void buildsWhatsAppWebhookUrl() {
        ChannelAccount channelAccount = channelAccount(ChannelProvider.WHATSAPP);

        assertThat(mapper("https://api.example.com").toDto(channelAccount).webhookUrl())
                .isEqualTo("https://api.example.com/whatsapp/webhook/" + channelAccount.getId());
    }

    @Test
    void hasNoWebhookUrlForProvidersWithoutWebhooks() {
        assertThat(
                        mapper("https://api.example.com")
                                .toDto(channelAccount(ChannelProvider.EMAIL))
                                .webhookUrl())
                .isNull();
    }
}
