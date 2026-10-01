package com.relayflow.api.workspace;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.authentication.PasswordResetService;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workspace.domain.ContactTagDefinition;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceMemberRepository;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @Mock private WorkspaceRepository workspaceRepository;

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock private UserRepository userRepository;

    @Mock private ChannelAccountRepository channelAccountRepository;

    @Mock private ContactRepository contactRepository;

    @Mock private ExternalIdentityRepository externalIdentityRepository;

    @Mock private ConversationRepository conversationRepository;

    @Mock private MessageRepository messageRepository;

    @Mock private WorkspaceMapper mapper;

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock private AiAgentConfigurationRepository aiAgentConfigurationRepository;

    @Mock private AiAgentInvocationLogRepository aiAgentInvocationLogRepository;

    @Mock private ConversationAiDraftRepository conversationAiDraftRepository;

    @Mock private PasswordResetService passwordResetService;

    private final ContactTagDefinition kycStatus =
            new ContactTagDefinition("kyc_status", "KYC status", List.of("pending", "verified"));

    private WorkspaceService service() {
        return new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                userRepository,
                channelAccountRepository,
                contactRepository,
                externalIdentityRepository,
                conversationRepository,
                messageRepository,
                mapper,
                workflowDefinitionRepository,
                aiAgentConfigurationRepository,
                aiAgentInvocationLogRepository,
                conversationAiDraftRepository,
                passwordResetService);
    }

    private void useStoredDefinitions() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setContactTagDefinitions(List.of(kycStatus));
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
    }

    @Test
    void refusesToRemoveAValueThatContactsStillHave() {
        useStoredDefinitions();
        when(contactRepository.countWithTag(WORKSPACE_ID, "kyc_status", "pending")).thenReturn(3L);

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateContactTagDefinitions(
                                                WORKSPACE_ID,
                                                List.of(
                                                        new ContactTagDefinition(
                                                                "kyc_status",
                                                                "KYC status",
                                                                List.of("verified")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 contact(s) have \"kyc_status\" set to \"pending\"");
    }

    @Test
    void removesAValueNoContactHas() {
        useStoredDefinitions();
        when(contactRepository.countWithTag(WORKSPACE_ID, "kyc_status", "pending")).thenReturn(0L);

        assertThatCode(
                        () ->
                                service()
                                        .updateContactTagDefinitions(
                                                WORKSPACE_ID,
                                                List.of(
                                                        new ContactTagDefinition(
                                                                "kyc_status",
                                                                "KYC status",
                                                                List.of("verified")))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsATagWithNoValues() {
        assertThatThrownBy(
                        () ->
                                service()
                                        .updateContactTagDefinitions(
                                                WORKSPACE_ID,
                                                List.of(
                                                        new ContactTagDefinition(
                                                                "tier", "Tier", List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs at least one value");
    }

    @Test
    void rejectsAColourThatIsNotInThePalette() {
        assertThatThrownBy(
                        () ->
                                service()
                                        .updateContactTagDefinitions(
                                                WORKSPACE_ID,
                                                List.of(
                                                        new ContactTagDefinition(
                                                                "kyc_status",
                                                                "KYC status",
                                                                List.of("verified"),
                                                                Map.of("verified", "gold")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown colour");
    }
}
