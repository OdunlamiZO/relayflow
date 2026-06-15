package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.repository.GuestRecoveryTokenRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import com.relayflow.api.messaging.MessagingService;
import com.relayflow.api.messaging.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tears down a single expired guest user and everything they own, in one transaction. */
@Component
public class GuestCleanupService {

    private final UserRepository userRepository;

    private final WorkspaceMemberRepository workspaceMemberRepository;

    private final GuestRecoveryTokenRepository guestRecoveryTokenRepository;

    private final MessagingService messagingService;

    public GuestCleanupService(
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            GuestRecoveryTokenRepository guestRecoveryTokenRepository,
            MessagingService messagingService) {
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.guestRecoveryTokenRepository = guestRecoveryTokenRepository;
        this.messagingService = messagingService;
    }

    @Transactional
    public void cleanupGuestUser(User user) {
        workspaceMemberRepository
                .findByUser(user.getId())
                .forEach(member -> messagingService.deleteWorkspace(member.getWorkspaceId()));

        guestRecoveryTokenRepository.deleteByUser(user.getId());
        userRepository.delete(user);
    }
}
