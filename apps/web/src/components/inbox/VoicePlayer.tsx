"use client";

// Docs: docs/codebase-components.md#voiceplayer
import { useRef, useState } from "react";

type Props = {
  src: string;
  durationSeconds: number | null;
};

export function VoicePlayer({ src, durationSeconds }: Props) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(durationSeconds ?? 0);
  const [failed, setFailed] = useState(false);

  async function togglePlayback() {
    const audio = audioRef.current;

    if (!audio) return;

    if (isPlaying) {
      audio.pause();
      setIsPlaying(false);

      return;
    }

    try {
      await audio.play();
      setFailed(false);
      setIsPlaying(true);
    } catch {
      setFailed(true);
    }
  }

  function seek(seconds: number) {
    if (audioRef.current) {
      audioRef.current.currentTime = seconds;
    }

    setCurrentTime(seconds);
  }

  return (
    <div className="flex w-64 max-w-full items-center gap-3">
      <audio
        ref={audioRef}
        src={src}
        preload="none"
        onLoadedMetadata={(event) => {
          if (Number.isFinite(event.currentTarget.duration)) {
            setDuration(event.currentTarget.duration);
          }
        }}
        onTimeUpdate={(event) =>
          setCurrentTime(event.currentTarget.currentTime)
        }
        onEnded={() => {
          setIsPlaying(false);
          setCurrentTime(0);
        }}
        onError={() => {
          setIsPlaying(false);
          setFailed(true);
        }}
      />

      <button
        type="button"
        onClick={togglePlayback}
        aria-label={isPlaying ? "Pause voice message" : "Play voice message"}
        className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-secondary text-neutral-100 transition-colors hover:bg-secondary-dark"
      >
        <span
          className="material-symbols-rounded text-[22px] leading-none"
          aria-hidden="true"
        >
          {isPlaying ? "pause" : "play_arrow"}
        </span>
      </button>

      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <input
          type="range"
          min={0}
          max={duration || 1}
          step={0.1}
          value={Math.min(currentTime, duration || 1)}
          onChange={(event) => seek(Number(event.target.value))}
          aria-label="Seek"
          className="h-1 w-full cursor-pointer accent-secondary"
        />
        <span className="text-[11px] tabular-nums text-neutral-500">
          {failed
            ? "Couldn't play this recording"
            : `${formatTime(currentTime)} / ${formatTime(duration)}`}
        </span>
      </div>
    </div>
  );
}

function formatTime(seconds: number) {
  const whole = Math.floor(seconds);

  return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, "0")}`;
}
