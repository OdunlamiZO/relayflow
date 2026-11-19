// Docs: docs/codebase-components.md#messagebubble
import Link from "next/link";

import { FormattedText } from "@/components/common/FormattedText";
import { Spinner } from "@/components/common/Spinner";
import { useMessageAudioUrl } from "@/hooks/use-message-audio-url";
import type { Message } from "@/lib/messaging-api";

import { VoicePlayer } from "./VoicePlayer";

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
  const audioUrl = useMessageAudioUrl(message);
  const audio = message.audio;

  return (
    <div
      className={`flex ${isOutbound ? "justify-end" : "justify-start"} ${
        audio ? "py-1" : ""
      }`}
    >
      <div
        className={`max-w-[85%] break-words rounded-2xl sm:max-w-[70%] ${
          audio ? "px-4 py-3" : "px-4 py-2.5"
        } ${
          isOutbound
            ? "rounded-br-sm bg-secondary text-neutral-100"
            : "rounded-bl-sm bg-neutral-300 text-neutral-800"
        }`}
      >
        {audio && (
          <>
            {audioUrl ? (
              <VoicePlayer
                src={audioUrl}
                durationSeconds={audio.durationSeconds}
              />
            ) : (
              <StatusLine icon="mic_off">Recording not kept</StatusLine>
            )}

            {audio.transcriptionStatus === "PENDING" && (
              <StatusLine spinner>Transcribing…</StatusLine>
            )}

            {audio.transcriptionStatus === "FAILED" && (
              <StatusLine icon="error">
                Couldn&apos;t transcribe this voice message
              </StatusLine>
            )}

            {audio.transcriptionStatus === "NO_PROVIDER" && (
              <StatusLine icon="info">
                Not transcribed.{" "}
                <Link
                  href={`/settings?workspaceId=${encodeURIComponent(message.workspaceId)}#providers`}
                  className="font-medium text-secondary hover:text-secondary-dark"
                >
                  Choose a transcription provider
                </Link>
              </StatusLine>
            )}
          </>
        )}

        {message.text && (
          <p
            className={`m-0 whitespace-pre-wrap text-sm leading-relaxed ${
              audio ? "mt-3 border-t border-neutral-400/40 pt-3" : ""
            }`}
          >
            <FormattedText text={message.text} />
          </p>
        )}

        <p
          className={`m-0 text-right text-xs ${audio ? "mt-2" : "mt-1"} ${
            isOutbound ? "text-neutral-100/70" : "text-neutral-500"
          }`}
        >
          {time}
        </p>
      </div>
    </div>
  );
}

type StatusLineProps = {
  icon?: string;
  spinner?: boolean;
  children: React.ReactNode;
};

function StatusLine({ icon, spinner, children }: StatusLineProps) {
  return (
    <div className="mt-2.5 flex items-center gap-2 text-xs text-neutral-500 first:mt-0">
      {spinner ? (
        <Spinner size="sm" />
      ) : (
        <span
          className="material-symbols-rounded text-[16px] leading-none"
          aria-hidden="true"
        >
          {icon}
        </span>
      )}
      <span>{children}</span>
    </div>
  );
}
