"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useApiKeys } from "@/hooks/use-api-keys";
import { useCreateApiKey } from "@/hooks/use-create-api-key";
import { useRevokeApiKey } from "@/hooks/use-revoke-api-key";
import { type ApiKey, type CreateApiKeyResponse } from "@/lib/messaging-api";

import { CreateApiKeyModal } from "./CreateApiKeyModal";
import { WebhookConfigPanel } from "./WebhookConfigPanel";

type Props = {
  workspaceId: string;
  canManageApiKeys: boolean;
  canManageWebhook: boolean;
};

export function IntegrationsPanel({
  workspaceId,
  canManageApiKeys,
  canManageWebhook,
}: Props) {
  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">Integrations</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Manage API keys for external access and configure webhook delivery.
        </p>
      </div>

      {canManageApiKeys && (
        <section className="mb-10">
          <div className="mb-4 flex items-center gap-2">
            <span
              className="material-symbols-rounded text-[20px] leading-none text-neutral-500"
              aria-hidden="true"
            >
              key
            </span>
            <h2 className="text-base font-semibold text-primary">API Keys</h2>
          </div>

          <p className="mb-4 text-sm text-neutral-500">
            API keys allow external systems to send messages via the public API.
            Keys authenticate with the{" "}
            <code className="rounded bg-neutral-100 px-1 text-[11px]">
              X-Api-Key
            </code>{" "}
            header.
          </p>

          <ApiKeysList workspaceId={workspaceId} />
        </section>
      )}

      {canManageApiKeys && canManageWebhook && (
        <hr className="mb-10 border-neutral-200" />
      )}

      {canManageWebhook && (
        <section>
          <div className="mb-4 flex items-center gap-2">
            <span
              className="material-symbols-rounded text-[20px] leading-none text-neutral-500"
              aria-hidden="true"
            >
              webhook
            </span>
            <h2 className="text-base font-semibold text-primary">Webhook</h2>
          </div>

          <p className="mb-4 text-sm text-neutral-500">
            Receive real-time event notifications to your endpoint. Each
            delivery is signed with HMAC-SHA256 so you can verify authenticity.
          </p>

          <WebhookConfigPanel workspaceId={workspaceId} />
        </section>
      )}
    </div>
  );
}

// ── API Keys sub-component ────────────────────────────────────────────────────

type ApiKeysListProps = {
  workspaceId: string;
};

function ApiKeysList({ workspaceId }: ApiKeysListProps) {
  const { data: keys, isLoading } = useApiKeys(workspaceId);
  const createApiKey = useCreateApiKey(workspaceId);
  const revokeApiKey = useRevokeApiKey(workspaceId);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createdKey, setCreatedKey] = useState<CreateApiKeyResponse | null>(
    null
  );
  const [revokeTarget, setRevokeTarget] = useState<ApiKey | null>(null);

  function handleCreate(name: string, expiresAt: string | null) {
    createApiKey.mutate(
      { name, expiresAt },
      {
        onSuccess: (data) => {
          setCreatedKey(data);
        },
      }
    );
  }

  function handleCloseModal() {
    setShowCreateModal(false);
    setCreatedKey(null);
  }

  function handleRevoke() {
    if (!revokeTarget) return;

    revokeApiKey.mutate(revokeTarget.id, {
      onSuccess: () => {
        setRevokeTarget(null);
      },
    });
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-6">
        <Spinner />
      </div>
    );
  }

  const activeKeys = keys?.filter((k) => !k.revokedAt) ?? [];

  return (
    <>
      {activeKeys.length === 0 ? (
        <div className="mb-4 rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
          <p className="text-sm text-neutral-400">No API keys yet.</p>
        </div>
      ) : (
        <ul className="mb-4 flex flex-col gap-2">
          {activeKeys.map((key) => (
            <ApiKeyRow
              key={key.id}
              apiKey={key}
              onRevoke={() => setRevokeTarget(key)}
            />
          ))}
        </ul>
      )}

      <button
        type="button"
        onClick={() => setShowCreateModal(true)}
        className="flex items-center gap-1.5 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100"
      >
        <span className="material-symbols-rounded text-[16px] leading-none">
          add
        </span>
        New API key
      </button>

      {(showCreateModal || createdKey) && (
        <CreateApiKeyModal
          isCreating={createApiKey.isPending}
          createdKey={createdKey}
          onSubmit={handleCreate}
          onClose={handleCloseModal}
        />
      )}

      {revokeTarget && (
        <ConfirmModal
          title={`Revoke "${revokeTarget.name}"?`}
          description="Any application using this key will immediately lose access. This cannot be undone."
          confirmLabel="Revoke"
          destructive
          isPending={revokeApiKey.isPending}
          onConfirm={handleRevoke}
          onCancel={() => setRevokeTarget(null)}
        />
      )}
    </>
  );
}

// ── Individual key row ────────────────────────────────────────────────────────

type ApiKeyRowProps = {
  apiKey: ApiKey;
  onRevoke: () => void;
};

function ApiKeyRow({ apiKey, onRevoke }: ApiKeyRowProps) {
  const isExpired =
    apiKey.expiresAt !== null && new Date(apiKey.expiresAt) < new Date();

  return (
    <li className="flex items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-3">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-primary">
            {apiKey.name}
          </span>

          {isExpired && (
            <span className="rounded-full bg-red-bg px-2 py-0.5 text-[11px] font-semibold text-red-text">
              Expired
            </span>
          )}
        </div>

        <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-neutral-400">
          <span>
            <code className="rounded bg-neutral-100 px-1">
              {apiKey.keyPrefix}…
            </code>
          </span>

          <span>
            Created{" "}
            {new Date(apiKey.createdAt).toLocaleDateString(undefined, {
              year: "numeric",
              month: "short",
              day: "numeric",
            })}
          </span>

          {apiKey.lastUsedAt && (
            <span>
              Last used{" "}
              {new Date(apiKey.lastUsedAt).toLocaleDateString(undefined, {
                year: "numeric",
                month: "short",
                day: "numeric",
              })}
            </span>
          )}

          {apiKey.expiresAt && !isExpired && (
            <span>
              Expires{" "}
              {new Date(apiKey.expiresAt).toLocaleDateString(undefined, {
                year: "numeric",
                month: "short",
                day: "numeric",
              })}
            </span>
          )}
        </div>
      </div>

      <button
        type="button"
        onClick={onRevoke}
        title="Revoke key"
        className="ml-3 flex-shrink-0 rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-red-bg hover:text-red-text"
      >
        <span className="material-symbols-rounded text-[18px] leading-none">
          delete
        </span>
      </button>
    </li>
  );
}
