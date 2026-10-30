package io.odunlamizo.relayflow.workspace;

// Docs: docs/codebase-components.md#workspaceinviteservice

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.authentication.domain.User;
import io.odunlamizo.relayflow.authentication.repository.UserRepository;
import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.email.EmailService;
import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceInvite;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceInvite.InviteStatus;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceMember;
import io.odunlamizo.relayflow.workspace.domain.WorkspacePermission;
import io.odunlamizo.relayflow.workspace.domain.WorkspaceRole;
import io.odunlamizo.relayflow.workspace.dto.CreateInviteRequest;
import io.odunlamizo.relayflow.workspace.dto.InvitePreviewResponse;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceInviteResponse;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceInviteRepository;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceMemberRepository;
import io.odunlamizo.relayflow.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceInviteService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceInviteService.class);

    private final WorkspaceInviteRepository inviteRepository;

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository memberRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final String webBaseUrl;

    private final int inviteExpiryDays;

    public WorkspaceInviteService(
            WorkspaceInviteRepository inviteRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl,
            @Value("${relayflow.invite.expiry-days}") int inviteExpiryDays) {
        this.inviteRepository = inviteRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.webBaseUrl = webBaseUrl;
        this.inviteExpiryDays = inviteExpiryDays;
    }

    @Transactional
    public WorkspaceInviteResponse createInvite(
            UUID workspaceId, UUID inviterId, CreateInviteRequest request) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        User inviter =
                userRepository
                        .findById(inviterId)
                        .orElseThrow(() -> new ResourceNotFoundException("Inviter not found"));

        WorkspacePermission.validateDependencies(request.permissions());

        userRepository
                .findByEmail(request.email())
                .flatMap(user -> memberRepository.findByWorkspaceAndUser(workspaceId, user.getId()))
                .ifPresent(
                        existingMember -> {
                            throw new IllegalArgumentException(
                                    "This person is already a member of the workspace");
                        });

        Instant now = Instant.now();
        Instant expiry = now.plus(inviteExpiryDays, ChronoUnit.DAYS);

        WorkspaceInvite invite =
                inviteRepository
                        .findActivePendingByWorkspaceAndEmail(workspaceId, request.email())
                        .map(
                                existing -> {
                                    existing.setExpiresAt(expiry);
                                    existing.setPermissions(request.permissions());
                                    existing.setToken(UUID.randomUUID());

                                    return inviteRepository.save(existing);
                                })
                        .orElseGet(
                                () -> {
                                    WorkspaceInvite newInvite = new WorkspaceInvite();
                                    newInvite.setWorkspaceId(workspaceId);
                                    newInvite.setEmail(request.email().toLowerCase());
                                    newInvite.setInvitedBy(inviterId);
                                    newInvite.setPermissions(request.permissions());
                                    newInvite.setExpiresAt(expiry);

                                    return inviteRepository.save(newInvite);
                                });

        String acceptUrl = webBaseUrl + "/invite?token=" + invite.getToken();

        String inviterName =
                inviter.getDisplayName() != null ? inviter.getDisplayName() : inviter.getEmail();

        emailService.sendInvite(request.email(), inviterName, workspace.getName(), acceptUrl);

        log.info(
                "Invite created {} {}",
                kv("workspaceId", workspaceId),
                kv("inviteId", invite.getId()));

        return toResponse(invite, inviterName);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInviteResponse> listPendingInvites(UUID workspaceId) {
        List<WorkspaceInvite> invites = inviteRepository.findPending(workspaceId);

        return invites.stream()
                .filter(invite -> invite.status() == InviteStatus.PENDING)
                .map(
                        pendingInvite -> {
                            String inviterName = resolveDisplayName(pendingInvite.getInvitedBy());

                            return toResponse(pendingInvite, inviterName);
                        })
                .toList();
    }

    @Transactional
    public void revokeInvite(UUID workspaceId, UUID inviteId) {
        WorkspaceInvite invite =
                inviteRepository
                        .findInWorkspace(workspaceId, inviteId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.status() != InviteStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invites can be revoked");
        }

        invite.setRevokedAt(Instant.now());
        inviteRepository.save(invite);

        log.info("Invite revoked {} {}", kv("workspaceId", workspaceId), kv("inviteId", inviteId));
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse getPreview(UUID token) {
        WorkspaceInvite invite =
                inviteRepository
                        .findByToken(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        Workspace workspace =
                workspaceRepository
                        .findById(invite.getWorkspaceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        String inviterName = resolveDisplayName(invite.getInvitedBy());

        return new InvitePreviewResponse(
                invite.getId(),
                invite.getWorkspaceId(),
                workspace.getName(),
                inviterName,
                invite.getEmail(),
                invite.getPermissions(),
                invite.status(),
                invite.getExpiresAt(),
                userRepository.findByEmail(invite.getEmail()).isPresent());
    }

    @Transactional
    public UUID acceptInvite(UUID token, UUID userId) {
        WorkspaceInvite invite =
                inviteRepository
                        .findByToken(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.status() == InviteStatus.ACCEPTED) {
            return invite.getWorkspaceId();
        }

        if (invite.status() == InviteStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invite has been revoked");
        }

        if (invite.status() == InviteStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invite has expired");
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This invite was sent to a different email address. "
                            + "Please log in with "
                            + invite.getEmail()
                            + " to accept.");
        }

        if (memberRepository.findByWorkspaceAndUser(invite.getWorkspaceId(), userId).isEmpty()) {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspaceId(invite.getWorkspaceId());
            member.setUserId(userId);
            member.setRole(WorkspaceRole.MEMBER);
            member.setPermissions(Set.copyOf(invite.getPermissions()));
            memberRepository.save(member);
        }

        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);

        log.info(
                "Invite accepted {} {} {}",
                kv("workspaceId", invite.getWorkspaceId()),
                kv("inviteId", invite.getId()),
                kv("userId", userId));

        return invite.getWorkspaceId();
    }

    private String resolveDisplayName(UUID userId) {
        return userRepository
                .findById(userId)
                .map(
                        user ->
                                user.getDisplayName() != null
                                        ? user.getDisplayName()
                                        : user.getEmail())
                .orElse("Unknown");
    }

    private WorkspaceInviteResponse toResponse(WorkspaceInvite invite, String inviterName) {

        return new WorkspaceInviteResponse(
                invite.getId(),
                invite.getEmail(),
                inviterName,
                invite.getPermissions(),
                invite.status(),
                invite.getCreatedAt(),
                invite.getExpiresAt());
    }
}
