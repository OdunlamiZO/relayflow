package io.odunlamizo.relayflow.workspace;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.odunlamizo.relayflow.agent.repository.AiAgentConfigurationRepository;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.authentication.PasswordResetService;
import io.odunlamizo.relayflow.authentication.repository.UserRepository;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.contact.repository.ExternalIdentityRepository;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workspace.domain.ContactTagDefinition;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceMemberRepository;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
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
