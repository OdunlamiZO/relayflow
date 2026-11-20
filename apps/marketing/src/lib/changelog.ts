// Docs: docs/codebase-components.md#changelog-page-and-changelogts

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
    version: "1.8.1",
    date: "2026-11-20",
    changes: {
      added: [
        "GET /workspaces/{workspaceId}/knowledge-sources/{sourceId}/fields fetches a source through its cache and lists its field paths with their current values.",
      ],
      changed: [
        "The variable picker on a knowledge answer lists each source's fields with their current values. Picking one inserts the full placeholder, such as {{sources.rates.usd}}.",
        "When a message is still unclear after the AI agent asked the customer to clarify, the agent sends the handoff message and hands the conversation to a person instead of asking again.",
      ],
      fixed: [
        "The AI agent is told to hand off whenever its reply says a person will take over, and that a request for a person counts in any words or language. Before, it could promise a team member without handing the conversation off.",
      ],
    },
  },
  {
    version: "1.8.0",
    date: "2026-11-20",
    changes: {
      added: [
        "Each AI agent has a handoff message, sent to the customer when the agent hands a conversation to a person without a reply of its own, such as when an escalation keyword matches.",
      ],
      changed: [
        "When the AI agent hands a conversation to a person, it first sends its reply or the handoff message, then stays quiet until a team member replies. Agents set to Draft only send nothing.",
        "A clarifying question from the AI agent is sent even at low confidence, instead of waiting as a draft.",
      ],
    },
  },
  {
    version: "1.7.1",
    date: "2026-11-20",
    changes: {
      added: [
        "Variable pickers on a knowledge source's header values, listing the workspace's secrets, and on knowledge answers, listing the knowledge sources.",
      ],
      fixed: [
        "The variable picker scrolls when it has more variables than fit, so every variable can be reached.",
      ],
    },
  },
  {
    version: "1.7.0",
    date: "2026-11-20",
    changes: {
      added: [
        "Knowledge sources in Settings → Knowledge sources: a GET request that returns JSON, with headers, {{secrets.NAME}} for API keys, and how long to reuse a response. A knowledge answer uses a live value as {{sources.NAME.field}}, and the agent sees the current value each time it replies.",
        "A value that can't be fetched reaches the AI agent as (unavailable). The agent is told not to guess it and to hand off when the customer needs it, and the reason is saved in the invocation log.",
        "Test a knowledge source before saving it, and preview a knowledge answer with live values.",
        "Run workflow in a conversation starts one of its AI agent's published workflows when the agent couldn't pick it. It clears the escalation and discards any draft.",
        "Endpoints under /workspaces/{workspaceId}/knowledge-sources and /workspaces/{workspaceId}/conversations/{conversationId}/agent-workflows.",
      ],
      changed: [
        "Closing a conversation cancels its running or waiting workflow runs, which show as CANCELLED with the reason.",
        "A knowledge source can't be renamed or deleted while an AI agent's knowledge base uses it.",
      ],
    },
  },
  {
    version: "1.6.0",
    date: "2026-11-19",
    changes: {
      added: [
        "When an Ask Question's hook rejects a voice reply, the workspace's chat model converts it to the expected value, such as \"five thousand\" to 5000, and the hook checks it again.",
      ],
    },
  },
  {
    version: "1.5.0",
    date: "2026-11-19",
    changes: {
      added: [
        "Each workspace chooses its chat model, transcription and storage providers and their models in Settings → Providers. Anything left on the instance default uses the Redis keys.",
        "A plugin whose environment variables are missing shows as not configured in Settings → Providers and in the API's startup logs.",
      ],
      changed: [
        "Breaking: nothing is chosen for you. With no chat model provider set for the agent, the workspace or the instance, the AI agent doesn't reply and conversations go to a person. Voice messages aren't transcribed or stored until a provider is chosen, even when only one is installed. Anthropic no longer defaults to claude-haiku-4-5, and transcription no longer defaults to whisper-1, so set a model for each provider.",
      ],
    },
  },
  {
    version: "1.4.0",
    date: "2026-11-19",
    changes: {
      added: [
        "Voice messages from Telegram and WhatsApp are transcribed and handled like text. The inbox plays the recording next to its transcript, and a voice message that isn't transcribed waits for a person.",
        "LLM, transcription and storage providers are plugins. The official ones ship with the API image, and you can add your own by putting a JAR in the plugins folder.",
      ],
    },
  },
  {
    version: "1.3.4",
    date: "2026-11-18",
    changes: {
      fixed: [
        "Conversations, messages, contacts and workflows can only be read or created by members of their workspace.",
      ],
    },
  },
  {
    version: "1.3.3",
    date: "2026-11-17",
    changes: {
      fixed: [
        'Logging in with a wrong password now shows "Invalid email or password." instead of an unexpected error.',
      ],
    },
  },
  {
    version: "1.3.2",
    date: "2026-11-15",
    changes: {
      fixed: [
        "When a login session expires, the web app now sends you to the login page instead of showing load errors.",
      ],
    },
  },
  {
    version: "1.3.1",
    date: "2026-11-15",
    changes: {
      fixed: [
        "AI agent replies no longer show a literal \\n to the customer when the model escapes its line breaks.",
      ],
    },
  },
  {
    version: "1.3.0",
    date: "2026-11-15",
    changes: {
      added: [
        'AI agent extraction fields have an "Ask if missing" option. Turn it off and the agent records the value when the customer mentions it, but never asks for it. Existing fields keep being asked for.',
      ],
    },
  },
  {
    version: "1.2.0",
    date: "2026-10-30",
    changes: {
      changed: [
        "Breaking: the OpenAI, Groq, and Ollama LLM providers are replaced by one OpenAI-compatible provider that works with any API in OpenAI's format, including OpenRouter, Together, DeepSeek, and vLLM. Set OPENAI_COMPATIBLE_BASE_URL and OPENAI_COMPATIBLE_API_KEY, and choose the model in Redis at platform:llm:openai-compatible:model. OPENAI_API_KEY and GROQ_API_KEY are no longer read. Agents set to OpenAI, Groq, or Ollama move to the new provider automatically.",
        "The API's heap can now use 75% of its container's memory limit, up from the JVM default of 25%. Override it with JAVA_OPTS.",
        "Import and Load sample only appear on an empty workflow, so an import can't replace an existing one.",
      ],
    },
  },
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
