"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useChannelAccounts } from "@/hooks/use-channel-accounts";
import { useDeleteChannelAccount } from "@/hooks/use-delete-channel-account";
import { useReconnectChannelAccount } from "@/hooks/use-reconnect-channel-account";
import { type ChannelAccount } from "@/lib/messaging-api";

import { ConnectTelegramForm } from "../inbox/ConnectTelegramForm";

const PROVIDER_LABEL: Record<string, string> = {
  TELEGRAM: "Telegram",
  WHATSAPP: "WhatsApp",
  INSTAGRAM: "Instagram",
  MESSENGER: "Messenger",
  WEBCHAT: "Web Chat",
  EMAIL: "Email",
  SMS: "SMS",
};

const PROVIDER_ICON: Record<string, string> = {
  TELEGRAM: "send",
  WHATSAPP: "chat",
  EMAIL: "mail",
  SMS: "sms",
  WEBCHAT: "language",
  INSTAGRAM: "photo_camera",
  MESSENGER: "chat_bubble",
};

type Props = {
  workspaceId: string;
};

export function ChannelsList({ workspaceId }: Props) {
  const { data: channels, isLoading } = useChannelAccounts(workspaceId);
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      {/* Page header */}
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">Channels</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Connect messaging channels to receive conversations in this workspace.
        </p>
      </div>

      {/* Connected channels */}
      <section className="mb-8">
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Connected
        </h2>

        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {!isLoading && (!channels || channels.length === 0) && (
          <div className="rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
            <p className="text-sm text-neutral-400">
              No channels connected yet.
            </p>
          </div>
        )}

        {channels && channels.length > 0 && (
          <ul className="flex flex-col gap-2">
            {channels.map((channel) => (
              <ChannelItem
                key={channel.id}
                channel={channel}
                workspaceId={workspaceId}
              />
            ))}
          </ul>
        )}
      </section>

      {/* Add channel */}
      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Add channel
        </h2>

        {showForm ? (
          <div className="rounded-xl border border-neutral-300 bg-neutral-100 p-5">
            <div className="mb-4 flex items-center gap-2">
              <span
                className="material-symbols-rounded text-[18px] text-[#229ED9]"
                aria-hidden="true"
              >
                send
              </span>
              <span className="text-sm font-semibold text-primary">
                Telegram
              </span>
            </div>

            <ConnectTelegramForm
              workspaceId={workspaceId}
              onSuccess={() => setShowForm(false)}
              onCancel={() => setShowForm(false)}
            />
          </div>
        ) : (
          <button
            onClick={() => setShowForm(true)}
            className="flex w-full items-center gap-3 rounded-xl border border-dashed border-neutral-300 bg-white px-4 py-4 text-left transition-colors hover:border-neutral-400 hover:bg-neutral-50"
          >
            <span className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-[#229ED9]/10">
              <span
                className="material-symbols-rounded text-[18px] text-[#229ED9]"
                aria-hidden="true"
              >
                send
              </span>
            </span>

            <div className="flex-1">
              <p className="text-sm font-medium text-neutral-800">
                Connect Telegram
              </p>
              <p className="mt-0.5 text-xs text-neutral-500">
                Receive messages from your own Telegram bot
              </p>
            </div>

            <span
              className="material-symbols-rounded text-[18px] text-neutral-400"
              aria-hidden="true"
            >
              add
            </span>
          </button>
        )}
      </section>
    </div>
  );
}

function ChannelItem({
  channel,
  workspaceId,
}: {
  channel: ChannelAccount;
  workspaceId: string;
}) {
  const { mutate: disconnect, isPending: isDisconnecting } =
    useDeleteChannelAccount(workspaceId);
  const { mutate: reconnect, isPending: isReconnecting } =
    useReconnectChannelAccount(workspaceId);
  const [showConfirm, setShowConfirm] = useState(false);

  const apiBaseUrl =
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  // Show the webhook URL for custom-bot channels so the owner can verify
  // what endpoint RelayFlow registered with Telegram on their behalf.
  // Shared-bot channels route through a single platform-level endpoint —
  // there is no per-channel webhook to display.
  const webhookUrl =
    channel.provider === "TELEGRAM" &&
    channel.status === "ACTIVE" &&
    !channel.shared
      ? `${apiBaseUrl}/api/telegram/webhook/${channel.id}`
      : null;

  return (
    <>
      <li className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <span className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-neutral-100">
              <span
                className="material-symbols-rounded text-[18px] text-secondary"
                aria-hidden="true"
              >
                {PROVIDER_ICON[channel.provider] ?? "hub"}
              </span>
            </span>

            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-primary">
                {channel.name}
              </p>
              <p className="text-xs text-neutral-500">
                {PROVIDER_LABEL[channel.provider]}
              </p>
            </div>
          </div>

          <div className="flex flex-shrink-0 items-center gap-3">
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
                channel.status === "ACTIVE"
                  ? "bg-green-bg text-green-text"
                  : "bg-neutral-100 text-neutral-500"
              }`}
            >
              {channel.status === "ACTIVE" ? "Active" : "Disabled"}
            </span>

            {channel.status === "ACTIVE" ? (
              <button
                onClick={() => setShowConfirm(true)}
                className="text-xs font-medium text-neutral-400 transition-colors hover:text-red-text"
              >
                Disconnect
              </button>
            ) : (
              <button
                onClick={() => reconnect(channel.id)}
                disabled={isReconnecting}
                className="text-xs font-medium text-secondary transition-colors hover:underline disabled:opacity-60"
              >
                {isReconnecting ? "…" : "Reconnect"}
              </button>
            )}
          </div>
        </div>

        {webhookUrl && (
          <div className="flex items-center gap-2 rounded-lg border border-neutral-100 bg-neutral-50 px-3 py-2">
            <span
              className="material-symbols-rounded flex-shrink-0 text-[13px] text-neutral-400"
              aria-hidden="true"
            >
              webhook
            </span>
            <p className="min-w-0 flex-1 truncate font-mono text-[11px] text-neutral-500">
              {webhookUrl}
            </p>
          </div>
        )}
      </li>

      {showConfirm && (
        <ConfirmModal
          title="Disconnect channel?"
          description="New messages will stop coming in. Your existing conversations and history are preserved. You can reconnect at any time."
          confirmLabel="Disconnect"
          destructive
          isPending={isDisconnecting}
          onConfirm={() =>
            disconnect(channel.id, {
              onSettled: () => setShowConfirm(false),
            })
          }
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </>
  );
}
