package com.relayflow.api.email;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends emails via the Resend REST API (<a href="https://resend.com">https://resend.com</a>).
 *
 * <p>When {@code resend.api-key} is blank the service falls back to a no-op: it logs the accept URL
 * at INFO level so developers can test the invite flow locally without an email provider.
 */
@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private static final String RESEND_API = "https://api.resend.com/emails";

    private final String apiKey;

    private final String fromAddress;

    private final RestClient restClient;

    public ResendEmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:RelayFlow <noreply@relayflow.io>}") String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.restClient = RestClient.create();
    }

    @Override
    public void sendInvite(String to, String inviterName, String workspaceName, String acceptUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info(
                    "[no-op email] Invite for {} to workspace '{}' — accept: {}",
                    to,
                    workspaceName,
                    acceptUrl);

            return;
        }

        String subject = inviterName + " invited you to " + workspaceName + " on RelayFlow";
        String html = buildHtml(inviterName, workspaceName, acceptUrl);

        try {
            restClient
                    .post()
                    .uri(RESEND_API)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(
                            Map.of(
                                    "from", fromAddress,
                                    "to", new String[] {to},
                                    "subject", subject,
                                    "html", html))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Invite email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendEmailVerification(String to, String name, String verifyUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[no-op email] Verify email for {} — link: {}", to, verifyUrl);

            return;
        }

        String subject = "Verify your RelayFlow account";
        String html = buildVerificationHtml(name, verifyUrl);

        try {
            restClient
                    .post()
                    .uri(RESEND_API)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(
                            Map.of(
                                    "from", fromAddress,
                                    "to", new String[] {to},
                                    "subject", subject,
                                    "html", html))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendDowngradeNotice(
            String to, String workspaceName, int lockedChannels, int lockedWorkflows) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info(
                    "[no-op email] Downgrade notice for {} — workspace='{}', lockedChannels={}, lockedWorkflows={}",
                    to,
                    workspaceName,
                    lockedChannels,
                    lockedWorkflows);

            return;
        }

        String subject = "Your " + workspaceName + " subscription has ended";
        String html = buildDowngradeHtml(workspaceName, lockedChannels, lockedWorkflows);

        try {
            restClient
                    .post()
                    .uri(RESEND_API)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(
                            Map.of(
                                    "from", fromAddress,
                                    "to", new String[] {to},
                                    "subject", subject,
                                    "html", html))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Downgrade notice sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send downgrade notice to {}: {}", to, e.getMessage());
        }
    }

    private String buildDowngradeHtml(
            String workspaceName, int lockedChannels, int lockedWorkflows) {
        String resourceSummary = buildResourceSummary(lockedChannels, lockedWorkflows);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 16px;">
                    <tr><td align="center">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:12px;border:1px solid #e5e5e5;overflow:hidden;">
                        <tr>
                          <td style="padding:32px 32px 24px;">
                            <p style="margin:0 0 8px;font-size:13px;font-weight:600;color:#6b7280;letter-spacing:.06em;text-transform:uppercase;">RelayFlow</p>
                            <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;">Your subscription has ended</h1>
                            <p style="margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;">
                              Your PRO subscription for <strong>%s</strong> has expired and the workspace has been moved to the Free plan.
                            </p>
                            %s
                            <p style="margin:16px 0 28px;font-size:15px;color:#374151;line-height:1.6;">
                              To restore access, re-subscribe from your billing settings. Disabled resources will not be deleted — they can be re-enabled once you upgrade or remove others to stay within the Free plan limits.
                            </p>
                            <a href="https://app.relayflow.io/settings?tab=billing"
                               style="display:inline-block;background:#2e69ff;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;padding:12px 28px;border-radius:8px;">
                              View billing settings
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:20px 32px 28px;border-top:1px solid #f3f4f6;">
                            <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                              If you believe this is a mistake, contact us at support@relayflow.io.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """
                .formatted(workspaceName, resourceSummary);
    }

    private String buildResourceSummary(int lockedChannels, int lockedWorkflows) {
        if (lockedChannels == 0 && lockedWorkflows == 0) {
            return "";
        }

        StringBuilder items = new StringBuilder();

        if (lockedChannels > 0) {
            items.append("<li style=\"margin-bottom:4px;\">")
                    .append(lockedChannels)
                    .append(lockedChannels == 1 ? " channel account" : " channel accounts")
                    .append("</li>");
        }

        if (lockedWorkflows > 0) {
            items.append("<li style=\"margin-bottom:4px;\">")
                    .append(lockedWorkflows)
                    .append(lockedWorkflows == 1 ? " workflow" : " workflows")
                    .append("</li>");
        }

        return "<p style=\"margin:0 0 8px;font-size:15px;font-weight:600;color:#111827;\">The following were disabled:</p>"
                + "<ul style=\"margin:0 0 16px;padding-left:20px;font-size:15px;color:#374151;line-height:1.8;\">"
                + items
                + "</ul>";
    }

    private String buildVerificationHtml(String name, String verifyUrl) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 16px;">
                    <tr><td align="center">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:12px;border:1px solid #e5e5e5;overflow:hidden;">
                        <tr>
                          <td style="padding:32px 32px 24px;">
                            <p style="margin:0 0 8px;font-size:13px;font-weight:600;color:#6b7280;letter-spacing:.06em;text-transform:uppercase;">RelayFlow</p>
                            <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;">Verify your email address</h1>
                            <p style="margin:0 0 28px;font-size:15px;color:#374151;line-height:1.6;">
                              Hi %s, click the button below to verify your email and activate your RelayFlow account.
                            </p>
                            <a href="%s"
                               style="display:inline-block;background:#2e69ff;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;padding:12px 28px;border-radius:8px;">
                              Verify email
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:20px 32px 28px;border-top:1px solid #f3f4f6;">
                            <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                              This link expires in 24 hours. If you didn't create a RelayFlow account, you can ignore this email.
                              <br>Or copy this link: <a href="%s" style="color:#2e69ff;">%s</a>
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """
                .formatted(name, verifyUrl, verifyUrl, verifyUrl);
    }

    private String buildHtml(String inviterName, String workspaceName, String acceptUrl) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                </head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 16px;">
                    <tr><td align="center">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:12px;border:1px solid #e5e5e5;overflow:hidden;">
                        <tr>
                          <td style="padding:32px 32px 24px;">
                            <p style="margin:0 0 8px;font-size:13px;font-weight:600;color:#6b7280;letter-spacing:.06em;text-transform:uppercase;">RelayFlow</p>
                            <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;">You're invited to join %s</h1>
                            <p style="margin:0 0 28px;font-size:15px;color:#374151;line-height:1.6;">
                              <strong>%s</strong> has invited you to collaborate in the <strong>%s</strong> workspace on RelayFlow.
                            </p>
                            <a href="%s"
                               style="display:inline-block;background:#7c3aed;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;padding:12px 28px;border-radius:8px;">
                              Accept invite
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:20px 32px 28px;border-top:1px solid #f3f4f6;">
                            <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                              This invite expires in 7 days. If you weren't expecting this, you can ignore this email.
                              <br>Or copy this link: <a href="%s" style="color:#7c3aed;">%s</a>
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """
                .formatted(
                        workspaceName, inviterName, workspaceName, acceptUrl, acceptUrl, acceptUrl);
    }
}
