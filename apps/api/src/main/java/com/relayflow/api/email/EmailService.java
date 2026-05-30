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
}
