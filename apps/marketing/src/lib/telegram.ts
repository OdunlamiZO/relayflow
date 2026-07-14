import { configuration } from "@/lib/configuration";

const TELEGRAM_API_BASE_URL = "https://api.telegram.org";

async function send(text: string): Promise<void> {
  const url = `${TELEGRAM_API_BASE_URL}/bot${configuration.telegram.botToken}/sendMessage`;

  try {
    await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: configuration.telegram.chatId,
        text,
        parse_mode: "HTML",
      }),
    });
  } catch (error) {
    console.error("Failed to send Telegram notification:", error);
  }
}

export async function notifyAccessGrantNeeded(input: {
  reference: string;
  email: string;
  githubUsername: string;
  accessGrantToken: string;
}): Promise<void> {
  const grantUrl = `${configuration.marketingBaseUrl}/api/admin/grant-access?token=${input.accessGrantToken}`;

  const text =
    "New RelayFlow license sale — action needed\n\n" +
    `Order: ${input.reference}\n` +
    `Email: ${input.email}\n` +
    `GitHub username: ${input.githubUsername}\n\n` +
    "1. Add this GitHub account as a read-only collaborator on the " +
    "relayflow-api / relayflow-web / relayflow-migrate packages.\n" +
    "2. Then tap the link below to release the license key to the buyer:\n" +
    `<a href="${grantUrl}">Confirm access granted</a>`;

  await send(text);
}
