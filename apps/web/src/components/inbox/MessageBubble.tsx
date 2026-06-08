import type { Message } from "@/lib/messaging-api";

type Props = {
  message: Message;
};

function formatMessageTime(isoString: string): string {
  return new Date(isoString).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function MessageBubble({ message }: Props) {
  const isOutbound = message.direction === "OUTBOUND";
  const time = formatMessageTime(message.createdAt);

  return (
    <div className={`flex ${isOutbound ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[85%] break-words rounded-2xl px-4 py-2.5 sm:max-w-[70%] ${
          isOutbound
            ? "rounded-br-sm bg-secondary text-neutral-100"
            : "rounded-bl-sm bg-neutral-300 text-neutral-800"
        }`}
      >
        {message.text && (
          <p className="m-0 whitespace-pre-wrap text-sm leading-relaxed">
            {message.text}
          </p>
        )}

        <p
          className={`m-0 mt-1 text-right text-xs ${
            isOutbound ? "text-neutral-100/70" : "text-neutral-500"
          }`}
        >
          {time}
        </p>
      </div>
    </div>
  );
}
