import type { Conversation } from "@/lib/messaging-api";

type Props = {
  conversation: Conversation;
  isSelected: boolean;
  onClick: () => void;
};

const STATUS_DOT: Record<string, string> = {
  OPEN: "bg-green-border",
  PENDING: "bg-yellow-border",
  CLOSED: "bg-neutral-400",
};

// Use a fixed locale so the output is identical between the Node.js server
// render and the client render, regardless of system/browser locale settings.
const LOCALE = "en-US";

function formatTimestamp(isoString: string | null): string {
  if (!isoString) {
    return "";
  }

  const date = new Date(isoString);
  const now = new Date();
  const diffDays = Math.floor(
    (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
  );

  if (diffDays === 0) {
    return date.toLocaleTimeString(LOCALE, {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  if (diffDays === 1) {
    return "Yesterday";
  }

  if (diffDays < 7) {
    return date.toLocaleDateString(LOCALE, { weekday: "short" });
  }

  return date.toLocaleDateString(LOCALE, { month: "short", day: "numeric" });
}

export function ConversationItem({ conversation, isSelected, onClick }: Props) {
  const timestamp = formatTimestamp(
    conversation.lastMessageAt ?? conversation.createdAt
  );
  const dotClass = STATUS_DOT[conversation.status] ?? "bg-neutral-400";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full px-4 py-3 text-left transition-colors hover:bg-neutral-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-secondary ${
        isSelected ? "border-r-2 border-r-secondary bg-blue-bg" : ""
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span
              className={`h-2 w-2 flex-shrink-0 rounded-full ${dotClass}`}
            />
            <span className="truncate text-sm font-semibold text-neutral-800">
              {conversation.contactDisplayName ?? "Unknown contact"}
            </span>

            {conversation.escalatedAt && (
              <span
                className="material-symbols-rounded flex-shrink-0 text-[14px] leading-none text-red-text"
                title={conversation.escalationReason ?? "Escalated"}
                aria-label="Escalated"
              >
                error
              </span>
            )}
          </div>

          <p className="mt-0.5 truncate text-xs text-neutral-500">
            {conversation.channelAccountName} · {conversation.channelProvider}
          </p>
        </div>

        {timestamp && (
          // suppressHydrationWarning: the timestamp is derived from `new Date()`
          // (current time) so the server-render value and client-hydration value
          // can legitimately differ by the round-trip latency.
          <span
            suppressHydrationWarning
            className="flex-shrink-0 text-xs text-neutral-500"
          >
            {timestamp}
          </span>
        )}
      </div>
    </button>
  );
}
