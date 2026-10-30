package io.odunlamizo.relayflow.messaging;

// Docs: docs/codebase-components.md#messagingmapper

import io.odunlamizo.relayflow.messaging.domain.Conversation;
import io.odunlamizo.relayflow.messaging.domain.Message;
import io.odunlamizo.relayflow.messaging.dto.ConversationResponse;
import io.odunlamizo.relayflow.messaging.dto.MessageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessagingMapper {

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "contact.id", target = "contactId")
    @Mapping(source = "contact.displayName", target = "contactDisplayName")
    @Mapping(source = "contact.tags", target = "contactTags")
    @Mapping(source = "channelAccount.id", target = "channelAccountId")
    @Mapping(source = "channelAccount.provider", target = "channelProvider")
    @Mapping(source = "channelAccount.name", target = "channelAccountName")
    ConversationResponse toDto(Conversation conversation);

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "conversation.id", target = "conversationId")
    MessageResponse toDto(Message message);
}
