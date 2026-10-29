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

// Newest release first; each version must match a git tag and the date that tag was cut.
export const CHANGELOG_RELEASES: ChangelogRelease[] = [
  {
    version: "1.1.0",
    date: "2026-10-29",
    changes: {
      changed: [
        'Breaking: public API errors are now RFC 9457 problem details (application/problem+json) with a code to branch on, such as CONVERSATION_NOT_FOUND or INVALID_TAG_VALUE, instead of {"message", "timestamp"}. See the error table in the README.',
      ],
      fixed: [
        "A public API request with a missing or invalid API key returned an empty 403. It now returns 401 with MISSING_API_KEY or INVALID_API_KEY.",
        "A public API request with malformed JSON or an invalid ID returned 500. It now returns 400 with INVALID_REQUEST.",
      ],
    },
  },
  {
    version: "1.0.0",
    date: "2026-10-27",
    changes: {
      added: [
        "Shared inbox for Telegram and WhatsApp Business, with live updates, conversation assignment, and escalation badges.",
        "Contacts with custom fields, tags with a fixed list of values, merge, and delete.",
        "Contact access: accept messages from everyone, or only from whitelisted phone numbers.",
        "Workflow builder and execution engine, with run logs showing each step's input and output.",
        "Hooks: check and clean up a contact's reply to an Ask Question, or a value an AI agent extracts. Built-in hooks cover common formats, or write your own in FEEL.",
        "Workspace secrets for HTTP Request nodes, encrypted at rest and resolved only when the request is sent.",
        "AI agents with draft and auto-send modes, per-channel assignment, data extraction into contact fields, and escalation to a human. Supports Anthropic, OpenAI, Groq, and Ollama.",
        "Message formatting that works the same on WhatsApp and Telegram.",
        "Public API, API keys, and outbound webhooks.",
        "Multiple workspaces with member invites and granular permissions.",
        "Email and password or Google login, two-factor authentication, and password reset links.",
        "Structured logs for Datadog, Elastic, Graylog, and Loki, set with LOG_FORMAT. Every request gets an ID that appears in its log lines.",
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
