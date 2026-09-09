"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useDeleteWebhook } from "@/hooks/use-delete-webhook";
import { useRotateWebhookSecret } from "@/hooks/use-rotate-webhook-secret";
import { useSaveWebhook } from "@/hooks/use-save-webhook";
import { useWorkspaceWebhook } from "@/hooks/use-workspace-webhook";
import { errorMessage } from "@/lib/error-message";
import { type WebhookConfig, type WebhookEventType } from "@/lib/messaging-api";

const ALL_EVENTS: { value: WebhookEventType; payloadName: string }[] = [
  { value: "CONTACT_CREATED", payloadName: "contact.created" },
  { value: "CONTACT_UPDATED", payloadName: "contact.updated" },
];

type Props = {
  workspaceId: string;
};

export function WebhookConfigPanel({ workspaceId }: Props) {
  const { data: webhook, isLoading } = useWorkspaceWebhook(workspaceId);

  // Not in WebhookForm: it remounts on create, which would wipe this.
  const [revealedSecret, setRevealedSecret] = useState<string | null>(null);
  const [copiedSecret, setCopiedSecret] = useState(false);

  async function copySecret() {
    if (!revealedSecret) return;

    await navigator.clipboard.writeText(revealedSecret);
    setCopiedSecret(true);
    setTimeout(() => setCopiedSecret(false), 2000);
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <WebhookForm
        key={webhook?.id ?? "new"}
        workspaceId={workspaceId}
        webhook={webhook ?? null}
        onSecretRevealed={setRevealedSecret}
      />

      {/* Generated/rotated secret reveal */}
      {revealedSecret && (
        <div className="mt-4 rounded-xl border border-yellow-border bg-yellow-bg p-4">
          <p className="mb-2 text-sm font-medium text-yellow-text">
            New signing secret — copy now, it won&apos;t be shown again.
          </p>

          <div className="flex items-stretch gap-2 overflow-hidden rounded-xl border border-yellow-border bg-neutral-100">
            <code className="flex-1 overflow-x-auto p-3 text-xs text-neutral-700 select-all">
              {revealedSecret}
            </code>

            <button
              type="button"
              onClick={copySecret}
              className="flex flex-shrink-0 items-center gap-1.5 border-l border-yellow-border px-3 text-sm font-medium text-yellow-text transition-colors hover:bg-yellow-bg"
            >
              <span className="material-symbols-rounded text-[16px] leading-none">
                {copiedSecret ? "check" : "content_copy"}
              </span>
              {copiedSecret ? "Copied" : "Copy"}
            </button>
          </div>

          <p className="mt-3 text-xs text-yellow-text">
            Use it to verify the{" "}
            <code className="rounded bg-neutral-100 px-1 text-[11px]">
              X-RelayFlow-Signature
            </code>{" "}
            header on each delivery: it&apos;s{" "}
            <code className="rounded bg-neutral-100 px-1 text-[11px]">
              sha256=&lt;hex digest&gt;
            </code>
            , an HMAC-SHA256 of the raw request body keyed with this secret.
          </p>

          <button
            type="button"
            onClick={() => setRevealedSecret(null)}
            className="mt-2 text-xs text-yellow-text underline hover:no-underline"
          >
            I&apos;ve saved it, dismiss
          </button>
        </div>
      )}
    </>
  );
}

// ── Inner form — re-mounts when webhook id changes, so state is always fresh ──

type FormProps = {
  workspaceId: string;
  webhook: WebhookConfig | null;
  onSecretRevealed: (secret: string) => void;
};

function WebhookForm({ workspaceId, webhook, onSecretRevealed }: FormProps) {
  const saveWebhook = useSaveWebhook(workspaceId);
  const deleteWebhook = useDeleteWebhook(workspaceId);
  const rotateSecret = useRotateWebhookSecret(workspaceId);

  const [url, setUrl] = useState(webhook?.url ?? "");
  const [enabled, setEnabled] = useState(webhook?.enabled ?? false);
  const [events, setEvents] = useState<WebhookEventType[]>(
    webhook?.events ?? []
  );
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const isNew = !webhook;

  function toggleEvent(ev: WebhookEventType) {
    setEvents((prev) =>
      prev.includes(ev) ? prev.filter((e) => e !== ev) : [...prev, ev]
    );
  }

  function handleSave(e: React.FormEvent) {
    e.preventDefault();

    // Don't clear formError here — let it stay until the new outcome is known.
    saveWebhook.mutate(
      { url: url.trim(), enabled, events },
      {
        onSuccess: (data) => {
          setFormError(null);

          if (data.generatedSecret) {
            onSecretRevealed(data.generatedSecret);
          }
        },
        onError: (err) => {
          setFormError(errorMessage(err));
        },
      }
    );
  }

  function handleDelete() {
    deleteWebhook.mutate(undefined, {
      onSuccess: () => {
        setShowDeleteConfirm(false);
      },
    });
  }

  function handleRotate() {
    rotateSecret.mutate(undefined, {
      onSuccess: (data) => {
        setFormError(null);
        onSecretRevealed(data.secret);
      },
      onError: (err) => {
        setFormError(errorMessage(err));
      },
    });
  }

  return (
    <>
      <form onSubmit={handleSave} className="space-y-5">
        {/* Endpoint URL */}
        <div>
          <label
            htmlFor="webhook-url"
            className="mb-1.5 block text-sm font-medium text-neutral-700"
          >
            Endpoint URL
          </label>
          <input
            id="webhook-url"
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
        <div className="flex items-center justify-between pt-1">
          <div className="flex gap-2">
            {!isNew && (
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(true)}
                className="rounded-lg border border-red-text/30 px-3 py-1.5 text-sm font-medium text-red-text transition-colors hover:bg-red-bg"
              >
                Delete webhook
              </button>
            )}

            {!isNew && (
              <LoadingButton
                type="button"
                onClick={handleRotate}
                isLoading={rotateSecret.isPending}
                className="rounded-lg border border-neutral-200 px-3 py-1.5 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
              >
                Rotate secret
              </LoadingButton>
            )}
          </div>

          <LoadingButton
            type="submit"
            isLoading={saveWebhook.isPending}
            className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:opacity-60"
          >
            {isNew ? "Create webhook" : "Save changes"}
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
