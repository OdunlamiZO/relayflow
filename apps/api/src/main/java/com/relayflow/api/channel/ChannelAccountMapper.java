package com.relayflow.api.channel;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.dto.ChannelAccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ChannelAccountMapper {

    @Value("${relayflow.api.base-url:http://localhost:8080}")
    protected String apiBaseUrl;

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(target = "webhookUrl", expression = "java(webhookUrl(channelAccount))")
    public abstract ChannelAccountResponse toDto(ChannelAccount channelAccount);

    protected String webhookUrl(ChannelAccount channelAccount) {
        String baseUrl = apiBaseUrl.replaceAll("/+$", "");

        return switch (channelAccount.getProvider()) {
            case TELEGRAM -> baseUrl + "/telegram/webhook/" + channelAccount.getId();
            case WHATSAPP -> baseUrl + "/whatsapp/webhook/" + channelAccount.getId();
            default -> null;
        };
    }
}
