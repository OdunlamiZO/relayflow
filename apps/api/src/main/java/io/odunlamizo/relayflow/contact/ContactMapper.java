package io.odunlamizo.relayflow.contact;

import io.odunlamizo.relayflow.contact.domain.Contact;
import io.odunlamizo.relayflow.contact.domain.ExternalIdentity;
import io.odunlamizo.relayflow.contact.dto.ContactResponse;
import io.odunlamizo.relayflow.contact.dto.ExternalIdentityResponse;
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
