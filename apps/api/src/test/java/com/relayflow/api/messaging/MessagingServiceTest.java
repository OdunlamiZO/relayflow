package com.relayflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.channel.ChannelAccountService;
import com.relayflow.api.contact.ContactService;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.domain.EscalationType;
import com.relayflow.api.messaging.domain.Message;
import com.relayflow.api.messaging.domain.MessageDirection;
import com.relayflow.api.messaging.domain.MessageSenderType;
import com.relayflow.api.messaging.dto.CreateMessageRequest;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceMemberRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock private ConversationRepository conversationRepository;

    @Mock private MessageRepository messageRepository;

    @Mock private MessagingMapper mapper;

    @Mock private WorkspaceService workspaceService;

    @Mock private ContactService contactService;

    @Mock private ChannelAccountService channelAccountService;

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private ConversationAiDraftRepository conversationAiDraftRepository;

    private MessagingService service() {
        return new MessagingService(
                conversationRepository,
                messageRepository,
                mapper,
                workspaceService,
                contactService,
                channelAccountService,
                workspaceMemberRepository,
                eventPublisher,
                conversationAiDraftRepository);
    }

    @ParameterizedTest
    @EnumSource(EscalationType.class)
    void humanReplyClearsEveryEscalationType(EscalationType escalationType) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);
        conversation.setEscalatedAt(Instant.now());
        conversation.setEscalationReason("Earlier escalation");
        conversation.setEscalationType(escalationType);

        when(conversationRepository.findInWorkspace(conversation.getId(), workspace.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service()
                .createMessage(
                        workspace.getId(),
                        conversation.getId(),
                        new CreateMessageRequest(
                                MessageDirection.OUTBOUND,
                                MessageSenderType.AGENT,
                                "On it",
                                null,
                                Map.of()),
                        UUID.randomUUID());

        assertThat(conversation.getEscalatedAt()).isNull();
        assertThat(conversation.getEscalationReason()).isNull();
        assertThat(conversation.getEscalationType()).isNull();
    }
}
