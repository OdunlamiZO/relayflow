package com.relayflow.api.channel;

import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.dto.ChannelAccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChannelAccountMapper {

    @Mapping(source = "workspace.id", target = "workspaceId")
    ChannelAccountResponse toDto(ChannelAccount channelAccount);
}
