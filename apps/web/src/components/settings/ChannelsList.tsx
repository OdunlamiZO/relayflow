"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { CopyButton } from "@/components/common/CopyButton";
import { Spinner } from "@/components/common/Spinner";
import { ConnectTelegramForm } from "@/components/inbox/ConnectTelegramForm";
import { ConnectWhatsAppForm } from "@/components/inbox/ConnectWhatsAppForm";
import { useAuthentication } from "@/hooks/use-authentication";
import { useChannelAccounts } from "@/hooks/use-channel-accounts";
import { useDeleteChannelAccount } from "@/hooks/use-delete-channel-account";
import { useReconnectChannelAccount } from "@/hooks/use-reconnect-channel-account";
import { type ChannelAccount } from "@/lib/messaging-api";

const sharedBotUsername = process.env.NEXT_PUBLIC_SHARED_BOT_USERNAME ?? "";

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

type ActiveForm = "TELEGRAM" | "WHATSAPP" | null;

type Props = {
  workspaceId: string;
};

export function ChannelsList({ workspaceId }: Props) {
  const { data: channels, isLoading } = useChannelAccounts(workspaceId);
  const { isAnonymous } = useAuthentication();
  const [activeForm, setActiveForm] = useState<ActiveForm>(null);

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
      {!isAnonymous && (
        <section>
          <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Add channel
          </h2>

          {activeForm === "TELEGRAM" && (
            <div className="rounded-xl border border-neutral-300 bg-neutral-100 p-5">
              <div className="mb-4 flex items-center gap-2">
                <span
                  className="material-symbols-rounded text-[18px] text-telegram"
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
                onSuccess={() => setActiveForm(null)}
                onCancel={() => setActiveForm(null)}
              />
            </div>
          )}

          {activeForm === "WHATSAPP" && (
            <div className="rounded-xl border border-neutral-300 bg-neutral-100 p-5">
              <div className="mb-4 flex items-center gap-2">
                <span
                  className="material-symbols-rounded text-[18px] text-whatsapp"
                  aria-hidden="true"
                >
                  chat
                </span>
                <span className="text-sm font-semibold text-primary">
                  WhatsApp
                </span>
              </div>

              <ConnectWhatsAppForm
                workspaceId={workspaceId}
                onSuccess={() => setActiveForm(null)}
                onCancel={() => setActiveForm(null)}
              />
            </div>
          )}

          {activeForm === null && (
            <div className="flex flex-col gap-2">
              <ProviderButton
                icon="send"
                iconColor="text-telegram"
                iconBg="bg-telegram/10"
                label="Connect Telegram"
                description="Receive messages from your own Telegram bot"
                onClick={() => setActiveForm("TELEGRAM")}
              />

              <ProviderButton
                icon="chat"
                iconColor="text-whatsapp"
                iconBg="bg-whatsapp/10"
                label="Connect WhatsApp"
                description="Receive messages via the WhatsApp Business Cloud API"
                onClick={() => setActiveForm("WHATSAPP")}
              />
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function ProviderButton({
  icon,
  iconColor,
  iconBg,
  label,
  description,
  onClick,
}: {
  icon: string;
  iconColor: string;
  iconBg: string;
  label: string;
  description: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-xl border border-dashed border-neutral-300 bg-neutral-100 px-4 py-4 text-left transition-colors hover:border-neutral-400 hover:bg-neutral-200"
    >
      <span
        className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg ${iconBg}`}
      >
        <span
          className={`material-symbols-rounded text-[18px] ${iconColor}`}
          aria-hidden="true"
        >
          {icon}
        </span>
      </span>

      <div className="flex-1">
        <p className="text-sm font-medium text-neutral-800">{label}</p>
        <p className="mt-0.5 text-xs text-neutral-500">{description}</p>
      </div>

      <span
        className="material-symbols-rounded text-[18px] text-neutral-400"
        aria-hidden="true"
      >
        add
      </span>
    </button>
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

  // Show the webhook URL so the workspace owner knows what to configure externally.
  // Telegram: relay reads automatically — show for reference.
  // WhatsApp: must be manually pasted into Meta Developer Console.
  // Shared-bot channels route through a platform-level endpoint, no per-channel URL.
  const webhookUrl = (() => {
    if (!channel.shared && channel.status === "ACTIVE") {
      if (channel.provider === "TELEGRAM") {
        return `${apiBaseUrl}/telegram/webhook/${channel.id}`;
      }
      if (channel.provider === "WHATSAPP") {
        return `${apiBaseUrl}/whatsapp/webhook/${channel.id}`;
      }
    }

    return null;
  })();

  return (
    <>
      <li className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-neutral-100 p-4">
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

            {channel.shared && (
              <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-neutral-400">
                Guest mode only
              </span>
            )}

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
          <div className="flex items-start gap-2 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2">
            <span
              className="material-symbols-rounded mt-0.5 flex-shrink-0 text-[13px] text-neutral-400"
              aria-hidden="true"
            >
              webhook
            </span>
            <div className="min-w-0 flex-1">
              <p className="break-all font-mono text-[11px] text-neutral-500">
                {webhookUrl}
              </p>
              {channel.provider === "WHATSAPP" && (
                <p className="mt-1 text-[11px] text-neutral-400">
                  Paste this URL into your Meta Developer Console under
                  Webhooks.
                </p>
              )}
            </div>

            <CopyButton
              text={webhookUrl}
              className="mt-0.5 flex-shrink-0 text-neutral-400 transition-colors hover:text-secondary"
            />
          </div>
        )}

        {channel.shared && sharedBotUsername && (
          <div className="flex items-center gap-2 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-[11px] font-medium text-neutral-500">
            <a
              href={`https://t.me/${sharedBotUsername}?start=${workspaceId}`}
              target="_blank"
              rel="noopener noreferrer"
              className="flex min-w-0 flex-1 items-center gap-2 transition-colors hover:text-secondary"
            >
              <span
                className="material-symbols-rounded flex-shrink-0 text-[13px] text-telegram"
                aria-hidden="true"
              >
                send
              </span>
              Open @{sharedBotUsername} on Telegram to test this channel
            </a>

            <CopyButton
              text={`https://t.me/${sharedBotUsername}?start=${workspaceId}`}
            />
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
