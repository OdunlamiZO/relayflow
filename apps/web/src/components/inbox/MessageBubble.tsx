// Docs: docs/codebase-components.md#messagebubble
import { FormattedText } from "@/components/common/FormattedText";
import { useMessageAudioUrl } from "@/hooks/use-message-audio-url";
import type { Message, TranscriptionStatus } from "@/lib/messaging-api";

type Props = {
  message: Message;
};

const TRANSCRIPTION_NOTES: Partial<Record<TranscriptionStatus, string>> = {
  PENDING: "Transcribing…",
  FAILED: "Couldn't transcribe this voice message",
  NO_PROVIDER:
    "Not transcribed: choose a transcription provider in Settings → Providers",
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
  const audioUrl = useMessageAudioUrl(message);
  const transcriptionNote = message.audio
    ? TRANSCRIPTION_NOTES[message.audio.transcriptionStatus]
    : undefined;

  return (
    <div className={`flex ${isOutbound ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[85%] break-words rounded-2xl px-4 py-2.5 sm:max-w-[70%] ${
          isOutbound
            ? "rounded-br-sm bg-secondary text-neutral-100"
            : "rounded-bl-sm bg-neutral-300 text-neutral-800"
        }`}
      >
        {message.audio && (
          <p className="m-0 mb-1 text-xs font-medium text-neutral-500">
            🎤 Voice message
          </p>
        )}

        {audioUrl && (
          <audio
            controls
            preload="none"
            src={audioUrl}
            className="mb-1 h-9 w-64 max-w-full"
          />
        )}

        {transcriptionNote && (
          <p className="m-0 text-xs italic text-neutral-500">
            {transcriptionNote}
          </p>
        )}

        {message.text && (
          <p className="m-0 whitespace-pre-wrap text-sm leading-relaxed">
            <FormattedText text={message.text} />
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
