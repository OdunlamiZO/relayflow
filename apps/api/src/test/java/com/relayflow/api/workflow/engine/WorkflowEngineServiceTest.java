package com.relayflow.api.workflow.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.domain.Conversation;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.workflow.NodeType;
import com.relayflow.api.workflow.domain.WorkflowDefinition;
import com.relayflow.api.workflow.domain.WorkflowRun;
import com.relayflow.api.workflow.domain.WorkflowRunStatus;
import com.relayflow.api.workflow.domain.WorkflowRunStep;
import com.relayflow.api.workflow.domain.WorkflowRunStepStatus;
import com.relayflow.api.workflow.repository.WorkflowRunRepository;
import com.relayflow.api.workspace.domain.Workspace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceTest {

    @Mock private WorkflowRunRepository runRepository;

    @Mock private ConversationRepository conversationRepository;

    @Mock private ExternalIdentityRepository externalIdentityRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private ReplyRouter replyRouter;

    @Mock private WorkflowMessageSender messageSender;

    @Mock private NodeExecutor sendMessageExecutor;

    private WorkflowRun run;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setWorkspace(workspace);

        run = new WorkflowRun();
        run.setId(UUID.randomUUID());
        run.setWorkspace(workspace);
        run.setConversation(conversation);
        run.setWorkflowDefinition(new WorkflowDefinition());
        run.setStatus(WorkflowRunStatus.WAITING);
        run.setWaitingAtNodeId("question");
        run.setContextSnapshot(Map.of());

        when(runRepository.findWithDefinitionById(run.getId())).thenReturn(Optional.of(run));
    }

    private WorkflowEngineService service() {
        when(sendMessageExecutor.nodeType()).thenReturn(NodeType.SEND_MESSAGE);

        return new WorkflowEngineService(
                runRepository,
                conversationRepository,
                externalIdentityRepository,
                eventPublisher,
                List.of(sendMessageExecutor),
                replyRouter,
                messageSender);
    }

    private void useGraph(Map<String, Object> questionData, String... handles) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(Map.of("id", "question", "type", "waitForReply", "data", questionData));

        List<Map<String, Object>> edges = new ArrayList<>();

        for (String handle : handles) {
            nodes.add(Map.of("id", handle + "-target", "type", "sendMessage", "data", Map.of()));

            Map<String, Object> edge = new HashMap<>();
            edge.put("id", handle + "-edge");
            edge.put("source", "question");
            edge.put("target", handle + "-target");
            edge.put("sourceHandle", handle);
            edges.add(edge);
        }

        run.getWorkflowDefinition().setDraftGraph(Map.of("nodes", nodes, "edges", edges));
    }

    private WorkflowRunStep questionStep() {
        return run.getSteps().stream()
                .filter(step -> step.getNodeId().equals("question"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void noReplyFollowsTheNoReplyEdgeAndCompletesTheRun() {
        useGraph(Map.of("timeoutMinutes", 15), "noReply");
        when(sendMessageExecutor.execute(any(), any()))
                .thenReturn(NodeExecutionResult.next(Map.of()));

        service().expireWaitingRun(run.getId());

        assertThat(questionStep().getStatus()).isEqualTo(WorkflowRunStepStatus.FAILED);
        assertThat(questionStep().getErrorMessage()).isEqualTo("No reply within 15 minutes");
        verify(sendMessageExecutor).execute(any(), any());
        assertThat(run.getStatus()).isEqualTo(WorkflowRunStatus.COMPLETED);
    }

    @Test
    void noReplyOnAValidatedQuestionFallsBackToTheInvalidEdge() {
        useGraph(Map.of("validationHook", "builtin:number"), "valid", "invalid");
        when(sendMessageExecutor.execute(any(), any()))
                .thenReturn(NodeExecutionResult.next(Map.of()));

        service().expireWaitingRun(run.getId());

        verify(sendMessageExecutor)
                .execute(argThat(node -> node.id().equals("invalid-target")), any());
        verify(sendMessageExecutor, never())
                .execute(argThat(node -> node.id().equals("valid-target")), any());
        assertThat(run.getStatus()).isEqualTo(WorkflowRunStatus.COMPLETED);
    }

    @Test
    void noReplyWithNowhereToGoFailsTheRun() {
        useGraph(Map.of("timeoutMinutes", 15));

        service().expireWaitingRun(run.getId());

        verify(sendMessageExecutor, never()).execute(any(), any());
        assertThat(questionStep().getStatus()).isEqualTo(WorkflowRunStepStatus.FAILED);
        assertThat(run.getStatus()).isEqualTo(WorkflowRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("No reply within 15 minutes");
    }
}
