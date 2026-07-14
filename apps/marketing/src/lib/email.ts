import { configuration } from "@/lib/configuration";

const RESEND_API_URL = "https://api.resend.com/emails";

function buildLicenseKeyEmailHtml(licenseKey: string, expiresAt: Date): string {
  const expiryText = expiresAt.toISOString().slice(0, 10);

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body style="margin:0;padding:0;background:#f5f5f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
      <table width="100%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 16px;">
        <tr><td align="center">
          <table width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:12px;border:1px solid #e5e5e5;overflow:hidden;">
            <tr>
              <td style="padding:32px 32px 24px;">
                <p style="margin:0 0 8px;font-size:13px;font-weight:600;color:#6b7280;letter-spacing:.06em;text-transform:uppercase;">RelayFlow</p>
                <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;">Your self-hosted license key</h1>
                <p style="margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;">
                  Thanks for purchasing RelayFlow self-hosted. Your license key is valid until <strong>${expiryText}</strong>.
                </p>
                <p style="margin:0 0 16px;font-size:13px;color:#374151;line-height:1.6;word-break:break-all;background:#f3f4f6;border-radius:8px;padding:14px 16px;font-family:monospace;">
                  ${licenseKey}
                </p>
                <p style="margin:16px 0 0;font-size:15px;color:#374151;line-height:1.6;">
                  Set this as <code>RELAYFLOW_LICENSE_KEY</code> in your deployment's <code>.env</code> file. See the
                  <a href="${configuration.marketingBaseUrl}/docs/self-hosting" style="color:#2563eb;">self-hosting guide</a>
                  for the full setup walkthrough, including the exact <code>docker-compose.yml</code> and env files to use.
                </p>
              </td>
            </tr>
          </table>
        </td></tr>
      </table>
    </body>
    </html>
  `;
}

export async function sendLicenseKeyEmail(input: {
  to: string;
  licenseKey: string;
  expiresAt: Date;
}): Promise<void> {
  if (!configuration.email.apiKey) {
    console.info(
      `[no-op email] License key for ${input.to} — key: ${input.licenseKey}`
    );

    return;
  }

  try {
    await fetch(RESEND_API_URL, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${configuration.email.apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: configuration.email.from,
        to: [input.to],
        subject: "Your RelayFlow self-hosted license key",
        html: buildLicenseKeyEmailHtml(input.licenseKey, input.expiresAt),
      }),
    });
  } catch (error) {
    console.error(`Failed to send license key email to ${input.to}:`, error);
  }
}
