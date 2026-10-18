package com.relayflow.api.email;

public interface EmailService {

    void sendInvite(String to, String inviterName, String workspaceName, String acceptUrl);

    void sendEmailVerification(String to, String name, String verifyUrl);

    void sendPasswordReset(String to, String name, String resetUrl);

    void sendDowngradeNotice(
            String to, String workspaceName, int lockedChannels, int lockedWorkflows);
}
