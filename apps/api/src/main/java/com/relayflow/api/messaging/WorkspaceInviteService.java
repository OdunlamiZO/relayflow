package com.relayflow.api.messaging;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.email.EmailService;
import com.relayflow.api.messaging.domain.Workspace;
import com.relayflow.api.messaging.domain.WorkspaceInvite;
import com.relayflow.api.messaging.domain.WorkspaceInvite.InviteStatus;
import com.relayflow.api.messaging.domain.WorkspaceMember;
import com.relayflow.api.messaging.domain.WorkspacePermission;
import com.relayflow.api.messaging.domain.WorkspaceRole;
import com.relayflow.api.messaging.dto.CreateInviteRequest;
import com.relayflow.api.messaging.dto.InvitePreviewResponse;
import com.relayflow.api.messaging.dto.WorkspaceInviteResponse;
import com.relayflow.api.messaging.repository.WorkspaceInviteRepository;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import com.relayflow.api.messaging.repository.WorkspaceRepository;
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

    private static final int INVITE_EXPIRY_DAYS = 7;

    private final WorkspaceInviteRepository inviteRepository;

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository memberRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final String webBaseUrl;

    public WorkspaceInviteService(
            WorkspaceInviteRepository inviteRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl) {
        this.inviteRepository = inviteRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.webBaseUrl = webBaseUrl;
    }

    // --- Create / resend ---

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

        // Guard: target must not already be a member.
        userRepository
                .findByEmail(request.email())
                .flatMap(u -> memberRepository.findByWorkspaceIdAndUserId(workspaceId, u.getId()))
                .ifPresent(
                        m -> {
                            throw new IllegalArgumentException(
                                    "This person is already a member of the workspace");
                        });

        Instant now = Instant.now();
        Instant expiry = now.plus(INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);

        // If a pending invite already exists for this email, refresh it instead of creating a
        // duplicate.
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
                                    WorkspaceInvite i = new WorkspaceInvite();
                                    i.setWorkspaceId(workspaceId);
                                    i.setEmail(request.email().toLowerCase());
                                    i.setInvitedBy(inviterId);
                                    i.setPermissions(request.permissions());
                                    i.setExpiresAt(expiry);

                                    return inviteRepository.save(i);
                                });

        String acceptUrl = webBaseUrl + "/invite?token=" + invite.getToken();

        String inviterName =
                inviter.getDisplayName() != null ? inviter.getDisplayName() : inviter.getEmail();

        emailService.sendInvite(request.email(), inviterName, workspace.getName(), acceptUrl);

        log.info(
                "Invite created: id={}, workspace={}, email={}",
                invite.getId(),
                workspaceId,
                request.email());

        return toResponse(invite, inviterName);
    }

    // --- List pending ---

    @Transactional(readOnly = true)
    public List<WorkspaceInviteResponse> listPendingInvites(UUID workspaceId) {
        List<WorkspaceInvite> invites = inviteRepository.findPendingByWorkspaceId(workspaceId);

        return invites.stream()
                .filter(i -> i.status() == InviteStatus.PENDING)
                .map(
                        i -> {
                            String inviterName = resolveDisplayName(i.getInvitedBy());

                            return toResponse(i, inviterName);
                        })
                .toList();
    }

    // --- Revoke ---

    @Transactional
    public void revokeInvite(UUID workspaceId, UUID inviteId) {
        WorkspaceInvite invite =
                inviteRepository
                        .findByWorkspaceIdAndId(workspaceId, inviteId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.status() != InviteStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invites can be revoked");
        }

        invite.setRevokedAt(Instant.now());
        inviteRepository.save(invite);

        log.info("Invite revoked: id={}, workspace={}", inviteId, workspaceId);
    }

    // --- Preview (public) ---

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
                invite.getExpiresAt());
    }

    // --- Accept ---

    @Transactional
    public UUID acceptInvite(UUID token, UUID userId) {
        WorkspaceInvite invite =
                inviteRepository
                        .findByToken(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.status() == InviteStatus.ACCEPTED) {
            // Idempotent: already accepted — just return the workspace so the user can be
            // redirected.
            return invite.getWorkspaceId();
        }

        if (invite.status() == InviteStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invite has been revoked");
        }

        if (invite.status() == InviteStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invite has expired");
        }

        // The logged-in user's email must match the invite email.
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

        // Guard: already a member (idempotent).
        if (memberRepository
                .findByWorkspaceIdAndUserId(invite.getWorkspaceId(), userId)
                .isEmpty()) {
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
                "Invite accepted: id={}, userId={}, workspace={}",
                invite.getId(),
                userId,
                invite.getWorkspaceId());

        return invite.getWorkspaceId();
    }

    // --- Helpers ---

    private String resolveDisplayName(UUID userId) {
        return userRepository
                .findById(userId)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
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
