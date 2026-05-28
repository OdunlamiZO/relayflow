"use client";

import Link from "next/link";
import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useChannelAccounts } from "@/hooks/use-channel-accounts";
import { useDeleteChannelAccount } from "@/hooks/use-delete-channel-account";
import { useReconnectChannelAccount } from "@/hooks/use-reconnect-channel-account";
import { type ChannelAccount } from "@/lib/messaging-api";

import { ConnectTelegramForm } from "./ConnectTelegramForm";

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
    <div className="mx-auto max-w-2xl p-6 sm:p-8">
      <div className="mb-6">
        <Link
          href={`/inbox?workspaceId=${workspaceId}`}
          className="mb-4 inline-flex items-center gap-1 text-xs font-medium text-neutral-500 transition-colors hover:text-neutral-700"
        >
          <span
            className="material-symbols-rounded text-[14px]"
            aria-hidden="true"
          >
            arrow_back
          </span>
          Back to inbox
        </Link>

        <h1 className="text-lg font-bold text-primary">Channels</h1>
        <p className="mt-1 text-sm text-neutral-500">
          Connect messaging channels to receive conversations in this workspace.
        </p>
      </div>

      {/* Connected channels */}
      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
          Connected
        </h2>

        {isLoading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}

        {!isLoading && (!channels || channels.length === 0) && (
          <p className="text-sm text-neutral-500">No channels connected yet.</p>
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
      <section className="mt-8">
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-500">
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
            className="flex w-full items-center gap-3 rounded-xl border border-dashed border-neutral-300 bg-neutral-100 px-4 py-3.5 text-left transition-colors hover:border-neutral-400 hover:bg-neutral-200"
          >
            <span
              className="material-symbols-rounded text-[18px] text-[#229ED9]"
              aria-hidden="true"
            >
              send
            </span>
            <div>
              <p className="text-sm font-medium text-neutral-700">
                Connect Telegram
              </p>
              <p className="text-xs text-neutral-500">
                Receive messages from your Telegram bot
              </p>
            </div>
            <span
              className="material-symbols-rounded ml-auto text-[16px] text-neutral-400"
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
  const webhookUrl =
    channel.provider === "TELEGRAM" && channel.status === "ACTIVE"
      ? `${apiBaseUrl}/api/telegram/webhook/${channel.id}`
      : null;

  return (
    <>
      <li className="flex flex-col gap-2 rounded-xl border border-neutral-300 bg-neutral-100 p-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-2.5">
            <span
              className="material-symbols-rounded shrink-0 text-[18px] text-secondary"
              aria-hidden="true"
            >
              {PROVIDER_ICON[channel.provider] ?? "hub"}
            </span>

            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-primary">
                {channel.name}
              </p>
              <p className="text-xs text-neutral-500">
                {PROVIDER_LABEL[channel.provider]}
              </p>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2">
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
                channel.status === "ACTIVE"
                  ? "bg-green-bg text-green-text"
                  : "bg-neutral-200 text-neutral-500"
              }`}
            >
              {channel.status.toLowerCase()}
            </span>

            {channel.status === "ACTIVE" ? (
              <button
                onClick={() => setShowConfirm(true)}
                className="text-xs font-medium text-neutral-500 hover:text-red-600"
              >
                Disconnect
              </button>
            ) : (
              <button
                onClick={() => reconnect(channel.id)}
                disabled={isReconnecting}
                className="text-xs font-medium text-secondary hover:underline disabled:opacity-60"
              >
                {isReconnecting ? "…" : "Reconnect"}
              </button>
            )}
          </div>
        </div>

        {webhookUrl && (
          <div className="flex items-center gap-1.5 rounded-lg border border-neutral-200 bg-white px-2.5 py-1.5">
            <span
              className="material-symbols-rounded shrink-0 text-[13px] text-neutral-400"
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
