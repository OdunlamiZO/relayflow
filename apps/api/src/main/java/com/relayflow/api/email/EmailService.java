package com.relayflow.api.email;

/** Sends transactional emails. Implementations may be no-op when a provider is not configured. */
public interface EmailService {

    /**
     * Sends a workspace invite email.
     *
     * @param to recipient email address
     * @param inviterName display name of the person who sent the invite
     * @param workspaceName name of the workspace the recipient is being invited to
     * @param acceptUrl full URL the recipient should visit to accept the invite
     */
    void sendInvite(String to, String inviterName, String workspaceName, String acceptUrl);

    /**
     * Sends an email verification email to a newly registered user.
     *
     * @param to recipient email address
     * @param name display name of the recipient
     * @param verifyUrl full URL the recipient should visit to verify their email
     */
    void sendEmailVerification(String to, String name, String verifyUrl);

    /**
     * Notifies the workspace owner that their subscription has ended and excess resources were
     * locked.
     *
     * @param to owner's email address
     * @param workspaceName name of the affected workspace
     * @param lockedChannels number of channel accounts that were disabled
     * @param lockedWorkflows number of workflows that were disabled
     */
    void sendDowngradeNotice(
            String to, String workspaceName, int lockedChannels, int lockedWorkflows);
}
