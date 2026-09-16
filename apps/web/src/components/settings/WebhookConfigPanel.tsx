"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useCreateWebhook } from "@/hooks/use-create-webhook";
import { useDeleteWebhook } from "@/hooks/use-delete-webhook";
import { useRotateWebhookSecret } from "@/hooks/use-rotate-webhook-secret";
import { useUpdateWebhook } from "@/hooks/use-update-webhook";
import { useWebhooks } from "@/hooks/use-webhooks";
import { errorMessage } from "@/lib/error-message";
import { type WebhookConfig, type WebhookEventType } from "@/lib/messaging-api";

import { WebhookSecretModal } from "./WebhookSecretModal";

const ALL_EVENTS: { value: WebhookEventType; payloadName: string }[] = [
  { value: "CONTACT_CREATED", payloadName: "contact.created" },
  { value: "CONTACT_UPDATED", payloadName: "contact.updated" },
];

function webhookLabel(url: string): string {
  try {
    return new URL(url).hostname || url;
  } catch {
    return url || "Webhook";
  }
}

type Props = {
  workspaceId: string;
};

export function WebhookConfigPanel({ workspaceId }: Props) {
  const { data: webhooks = [], isLoading } = useWebhooks(workspaceId);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  // Not lifted into WebhookForm: forms remount on tab switch/create/delete, which would wipe this.
  const [revealedSecret, setRevealedSecret] = useState<string | null>(null);

  const selected =
    webhooks.find((webhook) => webhook.id === selectedId) ??
    webhooks[0] ??
    null;

  const showingNew = isCreatingNew || (!selected && !isLoading);

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      {webhooks.length > 0 && (
        <div className="mb-4 flex flex-wrap gap-2">
          {webhooks.map((webhook) => (
            <WebhookTab
              key={webhook.id}
              label={webhookLabel(webhook.url)}
              enabled={webhook.enabled}
              active={!showingNew && webhook.id === selected?.id}
              onClick={() => {
                setSelectedId(webhook.id);
                setIsCreatingNew(false);
              }}
            />
          ))}

          <button
            type="button"
            onClick={() => setIsCreatingNew(true)}
            className={`flex items-center gap-1 rounded-full border border-dashed px-3 py-1.5 text-xs font-medium transition-colors ${
              showingNew
                ? "border-secondary bg-secondary/10 text-secondary"
                : "border-neutral-300 text-neutral-500 hover:border-neutral-400 hover:text-neutral-700"
            }`}
          >
            <span
              className="material-symbols-rounded text-[14px] leading-none"
              aria-hidden="true"
            >
              add
            </span>
            New webhook
          </button>
        </div>
      )}

      <WebhookForm
        key={showingNew ? "new" : selected?.id}
        workspaceId={workspaceId}
        webhook={showingNew ? null : selected}
        onSecretRevealed={setRevealedSecret}
        onCreated={(id) => {
          setSelectedId(id);
          setIsCreatingNew(false);
        }}
        onCancel={
          webhooks.length > 0 ? () => setIsCreatingNew(false) : undefined
        }
      />

      {revealedSecret && (
        <WebhookSecretModal
          secret={revealedSecret}
          onClose={() => setRevealedSecret(null)}
        />
      )}
    </>
  );
}

function WebhookTab({
  label,
  enabled,
  active,
  onClick,
}: {
  label: string;
  enabled: boolean;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
        active
          ? "border-secondary bg-secondary/10 text-secondary"
          : "border-neutral-300 bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
      }`}
    >
      <span
        className={`h-1.5 w-1.5 flex-shrink-0 rounded-full ${
          enabled ? "bg-green-text" : "bg-neutral-400"
        }`}
        aria-hidden="true"
      />
      <span className="max-w-[9rem] truncate">{label}</span>
    </button>
  );
}

// ── One webhook's form — remounts on tab switch/create/delete, so state is always fresh ──

type FormProps = {
  workspaceId: string;
  webhook: WebhookConfig | null;
  onSecretRevealed: (secret: string) => void;
  onCreated: (id: string) => void;
  onCancel?: () => void;
};

function WebhookForm({
  workspaceId,
  webhook,
  onSecretRevealed,
  onCreated,
  onCancel,
}: FormProps) {
  const isNew = !webhook;

  const createWebhook = useCreateWebhook(workspaceId);
  const updateWebhook = useUpdateWebhook(workspaceId, webhook?.id ?? "");
  const deleteWebhook = useDeleteWebhook(workspaceId, webhook?.id ?? "");
  const rotateSecret = useRotateWebhookSecret(workspaceId, webhook?.id ?? "");

  const [url, setUrl] = useState(webhook?.url ?? "");
  const [enabled, setEnabled] = useState(webhook?.enabled ?? false);
  const [events, setEvents] = useState<WebhookEventType[]>(
    webhook?.events ?? []
  );
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const isSaving = createWebhook.isPending || updateWebhook.isPending;

  const isUnchanged =
    !isNew &&
    (webhook?.url ?? "") === url.trim() &&
    (webhook?.enabled ?? false) === enabled &&
    JSON.stringify(webhook?.events ?? []) === JSON.stringify(events);

  function toggleEvent(ev: WebhookEventType) {
    setEvents((prev) =>
      prev.includes(ev) ? prev.filter((e) => e !== ev) : [...prev, ev]
    );
  }

  function handleSave(e: React.FormEvent) {
    e.preventDefault();

    const request = { url: url.trim(), enabled, events };

    // Don't clear formError here — let it stay until the new outcome is known.
    if (isNew) {
      createWebhook.mutate(request, {
        onSuccess: (data) => {
          setFormError(null);
          onCreated(data.id);

          if (data.generatedSecret) {
            onSecretRevealed(data.generatedSecret);
          }
        },
        onError: (err) => setFormError(errorMessage(err)),
      });

      return;
    }

    updateWebhook.mutate(request, {
      onSuccess: () => setFormError(null),
      onError: (err) => setFormError(errorMessage(err)),
    });
  }

  function handleDelete() {
    deleteWebhook.mutate(undefined, {
      onSuccess: () => setShowDeleteConfirm(false),
    });
  }

  function handleRotate() {
    rotateSecret.mutate(undefined, {
      onSuccess: (data) => {
        setFormError(null);
        onSecretRevealed(data.secret);
      },
      onError: (err) => setFormError(errorMessage(err)),
    });
  }

  return (
    <>
      <form onSubmit={handleSave} className="space-y-5">
        {/* Endpoint URL */}
        <div>
          <label
            htmlFor={`webhook-url-${webhook?.id ?? "new"}`}
            className="mb-1.5 block text-sm font-medium text-neutral-700"
          >
            Endpoint URL
          </label>
          <input
            id={`webhook-url-${webhook?.id ?? "new"}`}
            type="url"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://your-app.com/webhooks/relayflow"
            required
            className="w-full rounded-xl border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary"
          />
        </div>

        {/* Events */}
        <div>
          <p className="mb-2 text-sm font-medium text-neutral-700">Events</p>

          <div className="flex flex-col gap-2">
            {ALL_EVENTS.map((ev) => (
              <label
                key={ev.value}
                className="flex cursor-pointer items-center gap-3"
              >
                <input
                  type="checkbox"
                  checked={events.includes(ev.value)}
                  onChange={() => toggleEvent(ev.value)}
                  className="h-4 w-4 rounded border-neutral-300 accent-secondary"
                />
                <span className="font-mono text-sm text-neutral-700">
                  {ev.payloadName}
                </span>
              </label>
            ))}
          </div>
        </div>

        {/* Enabled toggle */}
        <div>
          <label className="flex cursor-pointer items-center justify-between">
            <span className="text-sm font-medium text-neutral-700">
              Enabled
            </span>

            <button
              type="button"
              role="switch"
              aria-checked={enabled}
              onClick={() => setEnabled((v) => !v)}
              className={`relative inline-flex h-6 w-11 flex-shrink-0 items-center rounded-full transition-colors ${
                enabled ? "bg-green-text" : "bg-neutral-300"
              }`}
            >
              <span
                className={`inline-block h-4 w-4 translate-x-1 rounded-full bg-neutral-100 shadow transition-transform ${
                  enabled ? "translate-x-6" : "translate-x-1"
                }`}
              />
            </button>
          </label>
        </div>

        {formError && <p className="text-sm text-red-text">{formError}</p>}

        {/* Actions */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
          <div className="flex flex-wrap gap-2">
            {isNew ? (
              onCancel && (
                <button
                  type="button"
                  onClick={onCancel}
                  className="rounded-lg border border-neutral-200 px-3 py-1.5 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100"
                >
                  Cancel
                </button>
              )
            ) : (
              <>
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="rounded-lg border border-red-text/30 px-3 py-1.5 text-sm font-medium text-red-text transition-colors hover:bg-red-bg"
                >
                  Delete webhook
                </button>

                <LoadingButton
                  type="button"
                  onClick={handleRotate}
                  isLoading={rotateSecret.isPending}
                  className="rounded-lg border border-neutral-200 px-3 py-1.5 text-sm font-medium text-neutral-600 transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary disabled:opacity-60"
                >
                  Rotate secret
                </LoadingButton>
              </>
            )}
          </div>

          <LoadingButton
            type="submit"
            isLoading={isSaving}
            disabled={isUnchanged}
            className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Save
          </LoadingButton>
        </div>
      </form>

      {showDeleteConfirm && (
        <ConfirmModal
          title="Delete webhook?"
          description="All future events will stop being delivered to this endpoint. This cannot be undone."
          confirmLabel="Delete"
          destructive
          isPending={deleteWebhook.isPending}
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteConfirm(false)}
        />
      )}
    </>
  );
}
