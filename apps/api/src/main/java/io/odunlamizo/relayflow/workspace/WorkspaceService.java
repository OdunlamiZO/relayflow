package io.odunlamizo.relayflow.workspace;

// Docs: docs/codebase-components.md#workspaceservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.agent.repository.AiAgentConfigurationRepository;
import io.odunlamizo.relayflow.agent.repository.AiAgentInvocationLogRepository;
import io.odunlamizo.relayflow.agent.repository.ConversationAiDraftRepository;
import io.odunlamizo.relayflow.authentication.PasswordResetService;
import io.odunlamizo.relayflow.authentication.domain.User;
import io.odunlamizo.relayflow.authentication.repository.UserRepository;
import io.odunlamizo.relayflow.channel.repository.ChannelAccountRepository;
import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.contact.repository.ContactRepository;
import io.odunlamizo.relayflow.contact.repository.ExternalIdentityRepository;
import io.odunlamizo.relayflow.messaging.repository.ConversationRepository;
import io.odunlamizo.relayflow.messaging.repository.MessageRepository;
import io.odunlamizo.relayflow.workflow.repository.WorkflowDefinitionRepository;
import io.odunlamizo.relayflow.workspace.domain.ContactFieldDefinition;
import io.odunlamizo.relayflow.workspace.domain.ContactTagDefinition;
import io.odunlamizo.relayflow.workspace.domain.ReservedContactField;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceMember;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceRole;
import io.odunlamizo.relayflow.workspace.dto.CreateWorkspaceRequest;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceMemberResponse;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceResponse;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceMemberRepository;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
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
                "Workspace created {} {}",
                kv("workspaceId", workspace.getId()),
                kv("ownerUserId", ownerId));

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

    @Transactional
    public WorkspaceResponse updateContactTagDefinitions(
            UUID workspaceId, List<ContactTagDefinition> contactTagDefinitions) {
        Set<String> keys = new HashSet<>();

        for (ContactTagDefinition definition : contactTagDefinitions) {
            if (definition.key() == null || definition.key().isBlank()) {
                throw new IllegalArgumentException("Every contact tag needs a key.");
            }

            if (!keys.add(definition.key())) {
                throw new IllegalArgumentException(
                        "\"" + definition.key() + "\" is used by more than one contact tag.");
            }

            if (definition.values().isEmpty()
                    || definition.values().stream().anyMatch(String::isBlank)
                    || Set.copyOf(definition.values()).size() != definition.values().size()) {
                throw new IllegalArgumentException(
                        "\""
                                + definition.key()
                                + "\" needs at least one value, with no blanks or duplicates.");
            }

            for (Map.Entry<String, String> color : definition.colors().entrySet()) {
                if (!definition.values().contains(color.getKey())
                        || !ContactTagDefinition.COLORS.contains(color.getValue())) {
                    throw new IllegalArgumentException(
                            "\""
                                    + definition.key()
                                    + "\" has a colour for an unknown value or an unknown colour.");
                }
            }
        }

        Workspace workspace = getWorkspace(workspaceId);

        for (ContactTagDefinition existing : workspace.getContactTagDefinitions()) {
            List<String> keptValues =
                    contactTagDefinitions.stream()
                            .filter(definition -> definition.key().equals(existing.key()))
                            .findFirst()
                            .map(ContactTagDefinition::values)
                            .orElse(List.of());

            for (String value : existing.values()) {
                long contacts =
                        keptValues.contains(value)
                                ? 0
                                : contactRepository.countWithTag(
                                        workspaceId, existing.key(), value);

                if (contacts > 0) {
                    throw new IllegalArgumentException(
                            contacts
                                    + " contact(s) have \""
                                    + existing.key()
                                    + "\" set to \""
                                    + value
                                    + "\". Change their tag before removing it.");
                }
            }
        }

        workspace.setContactTagDefinitions(contactTagDefinitions);
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
                        .collect(Collectors.toMap(User::getId, user -> user));

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

        log.info(
                "Member invited {} {}", kv("workspaceId", workspaceId), kv("userId", user.getId()));

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

        if (role == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException(
                    "Ownership can only be transferred via the transfer-ownership endpoint");
        }

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

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the owner of the workspace");
        }

        workspaceMemberRepository.delete(member);

        log.info("Member removed {} {}", kv("workspaceId", workspaceId), kv("memberId", memberId));
    }

    public void generatePasswordResetForMember(UUID workspaceId, UUID memberId) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findInWorkspace(workspaceId, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        passwordResetService.issueForUser(member.getUserId());
    }

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
                "Ownership transferred {} {} {}",
                kv("workspaceId", workspaceId),
                kv("previousOwnerMemberId", currentOwner.getId()),
                kv("newOwnerMemberId", newOwner.getId()));
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        log.info("Soft-deleting workspace {}", kv("workspaceId", workspaceId));
        Instant now = Instant.now();
        messageRepository.softDeleteByWorkspace(workspaceId, now);
        conversationRepository.softDeleteByWorkspace(workspaceId, now);
        externalIdentityRepository.softDeleteByWorkspace(workspaceId, now);
        contactRepository.softDeleteByWorkspace(workspaceId, now);
        channelAccountRepository.softDeleteByWorkspace(workspaceId, now);
        workflowDefinitionRepository.softDeleteByWorkspace(workspaceId, now);
        workspaceMemberRepository.softDeleteByWorkspace(workspaceId, now);
        conversationAiDraftRepository.deleteByWorkspace(workspaceId);
        aiAgentInvocationLogRepository.deleteByWorkspace(workspaceId);
        aiAgentConfigurationRepository.deleteByWorkspace(workspaceId);
        workspaceRepository.deleteById(workspaceId);
    }

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
