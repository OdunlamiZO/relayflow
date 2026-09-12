package com.relayflow.api.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.domain.AiAgentConfiguration;
import com.relayflow.api.agent.domain.ExtractionField;
import com.relayflow.api.agent.llm.AgentLlmRequest;
import com.relayflow.api.channel.domain.ChannelAccount;
import com.relayflow.api.channel.domain.ChannelProvider;
import com.relayflow.api.contact.ReservedContactFieldResolver;
import com.relayflow.api.contact.domain.Contact;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.workspace.domain.Workspace;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAgentContextAssemblerTest {

    @Mock private MessageRepository messageRepository;

    @Mock private ExternalIdentityRepository externalIdentityRepository;

    @Mock private ReservedContactFieldResolver reservedContactFieldResolver;

    private AiAgentContextAssembler assembler() {
        return new AiAgentContextAssembler(
                messageRepository, externalIdentityRepository, reservedContactFieldResolver);
    }

    private Conversation conversation() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setDisplayName("Jane Doe");

        ChannelAccount channelAccount = new ChannelAccount();
        channelAccount.setProvider(ChannelProvider.WHATSAPP);

        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setContact(contact);
        conversation.setChannelAccount(channelAccount);
        conversation.setSessionStartedAt(Instant.now());

        return conversation;
    }

    private void stubInboundMessage(Conversation conversation, String text) {
        Message message = new Message();
        message.setDirection(MessageDirection.INBOUND);
        message.setSenderType(MessageSenderType.CONTACT);
        message.setText(text);

        when(messageRepository.findRecentByConversationSince(any(), any(), any(), any()))
                .thenReturn(List.of(message));
    }

    private String lastMessageContent(AgentLlmRequest request) {
        return request.messages().get(request.messages().size() - 1).content();
    }

    @Test
    void footerHasNoKnownOrMissingSectionsWhenNoExtractionFieldsConfigured() {
        Conversation conversation = conversation();
        stubInboundMessage(conversation, "Hi there");

        AiAgentConfiguration configuration = new AiAgentConfiguration();

        AgentLlmRequest request = assembler().assemble(configuration, conversation);

        assertThat(lastMessageContent(request))
                .endsWith("<context>Contact: Jane Doe | Channel: WHATSAPP</context>");
    }

    @Test
    void footerSplitsConfiguredFieldsIntoKnownAndMissing() {
        Conversation conversation = conversation();
        stubInboundMessage(conversation, "Hi there");
        conversation.getContact().getCustomFields().put("phone", "+234801234567");

        when(externalIdentityRepository.findByContact(conversation.getContact().getId()))
                .thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(conversation.getContact(), List.of()))
                .thenReturn(Map.of());

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setExtractionFields(
                List.of(
                        new ExtractionField("phone", ""),
                        new ExtractionField("firstName", ""),
                        new ExtractionField("lastName", ""),
                        new ExtractionField("email", "")));

        AgentLlmRequest request = assembler().assemble(configuration, conversation);

        assertThat(lastMessageContent(request))
                .endsWith(
                        "<context>Contact: Jane Doe | Channel: WHATSAPP"
                                + " | Already known: phone=+234801234567"
                                + " | Still missing: firstName, lastName, email</context>");
    }

    @Test
    void footerOmitsMissingSectionWhenEveryFieldIsKnown() {
        Conversation conversation = conversation();
        stubInboundMessage(conversation, "Hi there");
        conversation.getContact().getCustomFields().put("phone", "+234801234567");

        when(externalIdentityRepository.findByContact(conversation.getContact().getId()))
                .thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(conversation.getContact(), List.of()))
                .thenReturn(Map.of());

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setExtractionFields(List.of(new ExtractionField("phone", "")));

        AgentLlmRequest request = assembler().assemble(configuration, conversation);

        assertThat(lastMessageContent(request))
                .endsWith(
                        "<context>Contact: Jane Doe | Channel: WHATSAPP"
                                + " | Already known: phone=+234801234567</context>");
    }

    @Test
    void footerOmitsKnownSectionWhenEveryFieldIsMissing() {
        Conversation conversation = conversation();
        stubInboundMessage(conversation, "Hi there");

        when(externalIdentityRepository.findByContact(conversation.getContact().getId()))
                .thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(conversation.getContact(), List.of()))
                .thenReturn(Map.of());

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setExtractionFields(List.of(new ExtractionField("firstName", "")));

        AgentLlmRequest request = assembler().assemble(configuration, conversation);

        assertThat(lastMessageContent(request))
                .endsWith(
                        "<context>Contact: Jane Doe | Channel: WHATSAPP | Still missing:"
                                + " firstName</context>");
    }

    @Test
    void treatsAResolverDerivedValueAsKnown() {
        Conversation conversation = conversation();
        stubInboundMessage(conversation, "Hi there");

        when(externalIdentityRepository.findByContact(conversation.getContact().getId()))
                .thenReturn(List.of());
        when(reservedContactFieldResolver.resolve(conversation.getContact(), List.of()))
                .thenReturn(Map.of("displayName", "Jane Doe"));

        AiAgentConfiguration configuration = new AiAgentConfiguration();
        configuration.setExtractionFields(List.of(new ExtractionField("displayName", "")));

        AgentLlmRequest request = assembler().assemble(configuration, conversation);

        assertThat(lastMessageContent(request))
                .endsWith(
                        "<context>Contact: Jane Doe | Channel: WHATSAPP"
                                + " | Already known: displayName=Jane Doe</context>");
    }
}
