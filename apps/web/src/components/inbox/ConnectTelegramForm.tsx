"use client";

import { useState } from "react";

import { LoadingButton } from "@/components/common/LoadingButton";
import { useConnectTelegram } from "@/hooks/use-connect-telegram";

type Props = {
  workspaceId: string;
  onSuccess: () => void;
  onCancel: () => void;
};

export function ConnectTelegramForm({
  workspaceId,
  onSuccess,
  onCancel,
}: Props) {
  const [botToken, setBotToken] = useState("");
  const [name, setName] = useState("");
  const { mutate: connect, isPending } = useConnectTelegram(workspaceId);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    connect(
      { name: name.trim() || "Telegram Bot", botToken: botToken.trim() },
      { onSuccess }
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="channel-name"
        >
          Display name
        </label>
        <input
          id="channel-name"
          type="text"
          placeholder="e.g. Support Bot"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label
          className="text-xs font-medium text-neutral-700"
          htmlFor="bot-token"
        >
          Bot token
        </label>
        <input
          id="bot-token"
          type="password"
          required
          placeholder="1234567890:ABCdef..."
          value={botToken}
          onChange={(e) => setBotToken(e.target.value)}
          className="rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 font-mono text-sm text-neutral-800 placeholder-neutral-400 focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
        />
        <p className="text-xs text-neutral-500">
          Create a bot via{" "}
          <a
            href="https://t.me/BotFather"
            target="_blank"
            rel="noopener noreferrer"
            className="text-secondary underline underline-offset-2"
          >
            @BotFather
          </a>{" "}
          on Telegram and paste the token here. RelayFlow will register the
          webhook automatically.
        </p>
      </div>

      <div className="flex items-center gap-2">
        <LoadingButton
          type="submit"
          isLoading={isPending}
          disabled={!botToken.trim()}
          className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Connect bot
        </LoadingButton>

        <button
          type="button"
          onClick={onCancel}
          disabled={isPending}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
