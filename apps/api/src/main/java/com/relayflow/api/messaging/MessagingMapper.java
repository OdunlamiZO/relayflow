package com.relayflow.api.messaging;

import com.relayflow.api.messaging.domain.ChannelAccount;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.dto.ChannelAccountResponse;
import com.relayflow.api.messaging.dto.ContactResponse;
import com.relayflow.api.messaging.dto.ConversationResponse;
import com.relayflow.api.messaging.dto.ExternalIdentityResponse;
import com.relayflow.api.messaging.dto.MessageResponse;
import com.relayflow.api.messaging.dto.WorkspaceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessagingMapper {

    WorkspaceResponse toDto(Workspace workspace);

    @Mapping(source = "workspace.id", target = "workspaceId")
    ChannelAccountResponse toDto(ChannelAccount channelAccount);

    @Mapping(source = "workspace.id", target = "workspaceId")
    ContactResponse toDto(Contact contact);

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "contact.id", target = "contactId")
    @Mapping(source = "contact.displayName", target = "contactDisplayName")
    @Mapping(source = "channelAccount.id", target = "channelAccountId")
    @Mapping(source = "channelAccount.provider", target = "channelProvider")
    @Mapping(source = "channelAccount.name", target = "channelAccountName")
    ConversationResponse toDto(Conversation conversation);

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "conversation.id", target = "conversationId")
    MessageResponse toDto(Message message);

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "contact.id", target = "contactId")
    ExternalIdentityResponse toDto(ExternalIdentity externalIdentity);
}
