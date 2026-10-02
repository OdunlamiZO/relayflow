export type ChangeCategory = "added" | "changed" | "fixed" | "removed";

export type ChangelogRelease = {
  version: string;
  date: string;
  changes: Partial<Record<ChangeCategory, string[]>>;
};

export const CHANGE_CATEGORY_ORDER: ChangeCategory[] = [
  "added",
  "changed",
  "fixed",
  "removed",
];

export const CHANGE_CATEGORY_LABELS: Record<ChangeCategory, string> = {
  added: "Added",
  changed: "Changed",
  fixed: "Fixed",
  removed: "Removed",
};

// Newest release first. Each version must match a git tag (vX.Y.Z) and the
// date must be the day that tag was cut.
export const CHANGELOG_RELEASES: ChangelogRelease[] = [
  {
    version: "1.0.6",
    date: "2026-10-01",
    changes: {
      added: [
        "Contact access: accept messages from everyone, or only from whitelisted phone numbers, set in Settings. Telegram users are asked to share their phone number first.",
        "contact.deleted and contact.merged webhook events.",
        "Contact tags: define tags with a fixed list of values in settings, then set them on a contact's page, with the Set Contact Tag workflow node, or through the public API. Workflows can branch on them.",
      ],
      changed: [
        "Messages sent through the public API don't reopen a closed conversation.",
        "On phone-sized screens, workflow editing and the AI agent, integrations, and hooks settings aren't available; a message asks you to use a computer.",
      ],
      fixed: [
        "AI agents could start a workflow that wasn't published.",
        "Live inbox updates could stall for up to a minute every few minutes.",
        "On phones, the inbox message box was hidden behind the bottom navigation.",
        "When a hook rejected a value an AI agent extracted, such as a number written in words, the agent asked the contact again instead of converting it to the required format first.",
      ],
    },
  },
  {
    version: "1.0.5",
    date: "2026-10-01",
    changes: {
      added: [
        'Ask Question nodes have a "No reply" output, followed when the contact doesn\'t reply before the timeout.',
      ],
      changed: [
        'An unanswered Ask Question no longer fails the whole run. The step is marked failed and the run follows "No reply", or "Invalid" for a validated question.',
      ],
    },
  },
  {
    version: "1.0.4",
    date: "2026-09-30",
    changes: {
      added: [
        "Hooks: check and clean up a contact's reply to an open-ended Ask Question. Built-in hooks cover email, phone number, number, whole number, date, and web address, or write your own in FEEL.",
        "AI agent extraction fields can use hooks. A rejected value isn't saved, and the agent asks the contact to correct it.",
        "Each channel shows its webhook URL in settings.",
      ],
      changed: [
        "The web app reaches the API through its own /backend path, so login works when the web app and API are on different domains.",
        "Breaking: the Google redirect URI is now <web address>/backend/login/oauth2/code/google. If Google login is enabled, add it in Google Cloud before upgrading, or Google sign-in fails with redirect_uri_mismatch. See Upgrades in the self-hosting docs.",
      ],
      fixed: [
        "An escalation caused by a failed LLM call or an internal error now clears once the AI agent responds successfully again.",
      ],
    },
  },
  {
    version: "1.0.3",
    date: "2026-09-21",
    changes: {
      added: [
        "Workspace secrets, managed in workspace settings. Reference them in HTTP Request nodes as {{secrets.NAME}}. Values are encrypted at rest and resolved only when the request is sent.",
      ],
      fixed: [
        "AI agents sent an empty reply when the LLM request failed. The conversation is now escalated to a human instead.",
      ],
    },
  },
  {
    version: "1.0.2",
    date: "2026-09-16",
    changes: {
      added: [
        "Multiple AI agents per workspace, with a workspace default and per-channel assignment.",
        "LLM provider setting per AI agent.",
        "Multiple webhooks per workspace.",
      ],
    },
  },
  {
    version: "1.0.1",
    date: "2026-09-12",
    changes: {
      added: ["Edit an AI draft before sending it."],
      changed: [
        "AI agents now receive the contact's known and missing fields as context.",
      ],
      fixed: [
        "Contact display name not updating when first or last name was set by a workflow or AI agent.",
        "Some API errors returned a generic error response instead of the specific one.",
      ],
      removed: ["Account deletion from the profile page."],
    },
  },
  {
    version: "1.0.0",
    date: "2026-09-09",
    changes: {
      added: [
        "Shared inbox for Telegram and WhatsApp Business.",
        "Workflow builder and execution engine.",
        "AI agents with draft and auto-send modes. Supports Anthropic, OpenAI, and Groq.",
        "Public API, API keys, and outbound webhooks.",
        "Multiple workspaces with member invites.",
        "Email verification and two-factor authentication.",
        "Docker Compose deployment with automatic TLS via Caddy.",
      ],
    },
  },
];

export function formatReleaseDate(date: string): string {
  return new Date(`${date}T00:00:00Z`).toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
    timeZone: "UTC",
  });
}
