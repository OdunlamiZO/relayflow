package com.relayflow.api.contact;

import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.domain.ExternalIdentity;
import com.relayflow.api.contact.dto.ContactResponse;
import com.relayflow.api.contact.dto.ExternalIdentityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ContactMapper {

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(target = "identities", ignore = true)
    ContactResponse toDto(Contact contact);

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "contact.id", target = "contactId")
    @Mapping(source = "channelAccount.id", target = "channelAccountId")
    ExternalIdentityResponse toDto(ExternalIdentity externalIdentity);
}
