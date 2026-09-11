package com.relayflow.api.workspace;

import com.relayflow.api.agent.repository.AiAgentConfigurationRepository;
import com.relayflow.api.agent.repository.AiAgentInvocationLogRepository;
import com.relayflow.api.agent.repository.ConversationAiDraftRepository;
import com.relayflow.api.authentication.PasswordResetService;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.channel.repository.ChannelAccountRepository;
import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.contact.repository.ContactRepository;
import com.relayflow.api.contact.repository.ExternalIdentityRepository;
import com.relayflow.api.messaging.repository.ConversationRepository;
import com.relayflow.api.messaging.repository.MessageRepository;
import com.relayflow.api.workflow.repository.WorkflowDefinitionRepository;
import com.relayflow.api.workspace.domain.ContactFieldDefinition;
import com.relayflow.api.workspace.domain.ReservedContactField;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.domain.WorkspaceMember;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import com.relayflow.api.workspace.domain.WorkspaceRole;
import com.relayflow.api.workspace.dto.CreateWorkspaceRequest;
import com.relayflow.api.workspace.dto.WorkspaceMemberResponse;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import com.relayflow.api.workspace.repository.WorkspaceMemberRepository;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final UserRepository userRepository;

    private final ChannelAccountRepository channelAccountRepository;

    private final ContactRepository contactRepository;

    private final ExternalIdentityRepository externalIdentityRepository;

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final WorkspaceMapper mapper;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final AiAgentConfigurationRepository aiAgentConfigurationRepository;

    private final AiAgentInvocationLogRepository aiAgentInvocationLogRepository;

    private final ConversationAiDraftRepository conversationAiDraftRepository;

    private final PasswordResetService passwordResetService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            ChannelAccountRepository channelAccountRepository,
            ContactRepository contactRepository,
            ExternalIdentityRepository externalIdentityRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WorkspaceMapper mapper,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            AiAgentConfigurationRepository aiAgentConfigurationRepository,
            AiAgentInvocationLogRepository aiAgentInvocationLogRepository,
            ConversationAiDraftRepository conversationAiDraftRepository,
            PasswordResetService passwordResetService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.channelAccountRepository = channelAccountRepository;
        this.contactRepository = contactRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.aiAgentConfigurationRepository = aiAgentConfigurationRepository;
        this.aiAgentInvocationLogRepository = aiAgentInvocationLogRepository;
        this.conversationAiDraftRepository = conversationAiDraftRepository;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UUID ownerId) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(ownerId);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        log.info(
                "Workspace created: id={}, name={}, owner={}",
                workspace.getId(),
                workspace.getName(),
                ownerId);

        return mapper.toDto(workspace);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, String name) {
        Workspace workspace = getWorkspace(workspaceId);
        workspace.setName(name);
        workspaceRepository.save(workspace);

        return mapper.toDto(workspace);
    }

    @Transactional
    public WorkspaceResponse updateContactFieldDefinitions(
            UUID workspaceId, List<ContactFieldDefinition> contactFieldDefinitions) {
        for (ContactFieldDefinition definition : contactFieldDefinitions) {
            if (ReservedContactField.isReserved(definition.key())) {
                throw new IllegalArgumentException(
                        "\"" + definition.key() + "\" is already a built-in field.");
            }
        }

        Workspace workspace = getWorkspace(workspaceId);
        workspace.setContactFieldDefinitions(contactFieldDefinitions);
        workspaceRepository.save(workspace);

        return mapper.toDto(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(UUID userId) {
        List<UUID> workspaceIds =
                workspaceMemberRepository.findByUser(userId).stream()
                        .map(WorkspaceMember::getWorkspaceId)
                        .toList();

        return workspaceRepository.findAllById(workspaceIds).stream()
                .sorted(Comparator.comparing(Workspace::getCreatedAt))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listWorkspaceMembers(UUID workspaceId) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace(workspaceId);

        Map<UUID, User> userMap =
                userRepository
                        .findAllById(members.stream().map(WorkspaceMember::getUserId).toList())
                        .stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream()
                .map(member -> toMemberResponse(member, userMap.get(member.getUserId())))
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse inviteWorkspaceMember(
            UUID workspaceId, String email, Set<WorkspacePermission> permissions) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "No user found with email: " + email));

        if (workspaceMemberRepository
                .findByWorkspaceAndUser(workspaceId, user.getId())
                .isPresent()) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        WorkspacePermission.validateDependencies(permissions);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getId());
        member.setRole(WorkspaceRole.MEMBER);
        member.setPermissions(permissions);
        workspaceMemberRepository.save(member);

        log.info("Member invited: userId={}, workspace={}", user.getId(), workspaceId);

        return toMemberResponse(member, user);
    }

    @Transactional
    public WorkspaceMemberResponse updateWorkspaceMember(
            UUID workspaceId,
            UUID memberId,
            WorkspaceRole role,
            Set<WorkspacePermission> permissions) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Guard: ownership can only be transferred, never set directly.
        if (role == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException(
                    "Ownership can only be transferred via the transfer-ownership endpoint");
        }

        // Guard: the owner cannot be demoted — workspaces have exactly one owner.
        if (member.getRole() == WorkspaceRole.OWNER && role != null) {
            throw new IllegalArgumentException("Cannot demote the owner of the workspace");
        }

        WorkspacePermission.validateDependencies(permissions);

        if (role != null) {
            member.setRole(role);
        }

        member.setPermissions(permissions);
        workspaceMemberRepository.save(member);

        User user = userRepository.findById(member.getUserId()).orElse(null);

        return toMemberResponse(member, user);
    }

    @Transactional
    public void removeWorkspaceMember(UUID workspaceId, UUID memberId) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Guard: the owner cannot be removed — workspaces have exactly one owner.
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the owner of the workspace");
        }

        workspaceMemberRepository.delete(member);

        log.info("Member removed: id={}, workspace={}", memberId, workspaceId);
    }

    public void generatePasswordResetForMember(UUID workspaceId, UUID memberId) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        passwordResetService.issueForUser(member.getUserId());
    }

    /**
     * Transfers ownership of the workspace to another member. The current owner is demoted to
     * {@link WorkspaceRole#MEMBER}; the target member becomes {@link WorkspaceRole#OWNER}.
     *
     * <p>Only the current owner can call this; {@code callerId} must be their user ID.
     */
    @Transactional
    public void transferOwnership(UUID workspaceId, UUID newOwnerMemberId, UUID callerId) {
        WorkspaceMember currentOwner =
                workspaceMemberRepository
                        .findByWorkspaceAndUser(workspaceId, callerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (currentOwner.getRole() != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the workspace owner can transfer ownership");
        }

        WorkspaceMember newOwner =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, newOwnerMemberId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Target member not found"));

        if (newOwner.getId().equals(currentOwner.getId())) {
            throw new IllegalArgumentException("Cannot transfer ownership to yourself");
        }

        currentOwner.setRole(WorkspaceRole.MEMBER);
        newOwner.setRole(WorkspaceRole.OWNER);

        workspaceMemberRepository.save(currentOwner);
        workspaceMemberRepository.save(newOwner);

        log.info(
                "Ownership transferred: workspace={}, from={}, to={}",
                workspaceId,
                currentOwner.getId(),
                newOwner.getId());
    }

    /**
     * Soft-deletes all data belonging to a workspace, then the workspace itself. No rows are
     * permanently removed — every table's {@code deleted_at} is stamped with the same instant. FK
     * order no longer matters for soft deletes, but preserved for clarity.
     */
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        log.info("Soft-deleting workspace and all associated data: {}", workspaceId);
        Instant now = Instant.now();
        messageRepository.softDeleteByWorkspace(workspaceId, now);
        conversationRepository.softDeleteByWorkspace(workspaceId, now);
        externalIdentityRepository.softDeleteByWorkspace(workspaceId, now);
        contactRepository.softDeleteByWorkspace(workspaceId, now);
        channelAccountRepository.softDeleteByWorkspace(workspaceId, now);
        workflowDefinitionRepository.softDeleteByWorkspace(workspaceId, now);
        workspaceMemberRepository.softDeleteByWorkspace(workspaceId, now);
        conversationAiDraftRepository.deleteByWorkspaceId(workspaceId);
        aiAgentInvocationLogRepository.deleteByWorkspaceId(workspaceId);
        aiAgentConfigurationRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.deleteById(workspaceId);
    }

    /**
     * Looks up a workspace by ID, or throws if it doesn't exist. Shared by other domain services.
     */
    public Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember member, User user) {

        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUserId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getDisplayName() : null,
                user != null ? user.getAvatarUrl() : null,
                member.getRole(),
                member.getPermissions(),
                member.getJoinedAt());
    }
}
