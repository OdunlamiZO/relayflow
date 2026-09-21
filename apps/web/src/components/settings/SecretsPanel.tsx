"use client";

import { useEffect, useRef, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useCreateSecret } from "@/hooks/use-create-secret";
import { useDeleteSecret } from "@/hooks/use-delete-secret";
import { useSecrets } from "@/hooks/use-secrets";
import { useUpdateSecret } from "@/hooks/use-update-secret";
import { errorMessage } from "@/lib/error-message";
import { type Secret } from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

export function SecretsPanel({ workspaceId }: Props) {
  const { data: secrets, isLoading } = useSecrets(workspaceId);
  const createSecret = useCreateSecret(workspaceId);
  const deleteSecret = useDeleteSecret(workspaceId);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<Secret | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Secret | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  function handleCreate(name: string, value: string) {
    setFormError(null);
    createSecret.mutate(
      { name, value },
      {
        onSuccess: () => setShowCreateModal(false),
        onError: (err) => setFormError(errorMessage(err)),
      }
    );
  }

  function handleDelete() {
    if (!deleteTarget) return;

    deleteSecret.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-6">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <div className="mb-4 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span
            className="material-symbols-rounded text-[20px] leading-none text-neutral-500"
            aria-hidden="true"
          >
            lock
          </span>
          <h2 className="text-base font-semibold text-primary">Secrets</h2>
        </div>

        <button
          type="button"
          onClick={() => {
            setFormError(null);
            setShowCreateModal(true);
          }}
          className="flex items-center gap-1 rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary"
        >
          <span
            className="material-symbols-rounded text-[14px] leading-none"
            aria-hidden="true"
          >
            add
          </span>
          New secret
        </button>
      </div>

      <p className="mb-4 text-sm text-neutral-500">
        Store credentials — API keys, tokens — for a workflow&apos;s HTTP
        Request node to use, without ever exposing the value itself. Reference
        one as{" "}
        <code className="rounded bg-neutral-100 px-1 text-[11px]">
          {"{{secrets.NAME}}"}
        </code>{" "}
        in a URL, header, or body.
      </p>

      {!secrets || secrets.length === 0 ? (
        <div className="mb-4 rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
          <p className="text-sm text-neutral-400">No secrets yet.</p>
        </div>
      ) : (
        <ul className="mb-4 flex flex-col gap-2">
          {secrets.map((secret) => (
            <SecretRow
              key={secret.id}
              secret={secret}
              onEdit={() => setEditTarget(secret)}
              onDelete={() => setDeleteTarget(secret)}
            />
          ))}
        </ul>
      )}

      {showCreateModal && (
        <SaveSecretModal
          mode="create"
          isSaving={createSecret.isPending}
          error={formError}
          onSubmit={handleCreate}
          onClose={() => setShowCreateModal(false)}
        />
      )}

      {editTarget && (
        <EditSecretModal
          workspaceId={workspaceId}
          secret={editTarget}
          onClose={() => setEditTarget(null)}
        />
      )}

      {deleteTarget && (
        <ConfirmModal
          title={`Delete "${deleteTarget.name}"?`}
          description="Any workflow referencing this secret will fail until it's removed from the workflow or a new secret with the same name is created. This cannot be undone."
          confirmLabel="Delete"
          destructive
          isPending={deleteSecret.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </>
  );
}

// ── Edit modal — owns its own mutation so it can carry its own error state ──

function EditSecretModal({
  workspaceId,
  secret,
  onClose,
}: {
  workspaceId: string;
  secret: Secret;
  onClose: () => void;
}) {
  const updateSecret = useUpdateSecret(workspaceId, secret.id);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(name: string, value: string) {
    setError(null);
    updateSecret.mutate(
      { name, value },
      {
        onSuccess: onClose,
        onError: (err) => setError(errorMessage(err)),
      }
    );
  }

  return (
    <SaveSecretModal
      mode="edit"
      initialName={secret.name}
      isSaving={updateSecret.isPending}
      error={error}
      onSubmit={handleSubmit}
      onClose={onClose}
    />
  );
}

// ── Create / rotate form modal ──────────────────────────────────────────────

function SaveSecretModal({
  mode,
  initialName = "",
  isSaving,
  error,
  onSubmit,
  onClose,
}: {
  mode: "create" | "edit";
  initialName?: string;
  isSaving: boolean;
  error: string | null;
  onSubmit: (name: string, value: string) => void;
  onClose: () => void;
}) {
  const [name, setName] = useState(initialName);
  const [value, setValue] = useState("");
  const firstFieldRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    firstFieldRef.current?.focus();
  }, []);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }

    document.addEventListener("keydown", onKeyDown);

    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!name.trim() || !value.trim()) return;

    onSubmit(name.trim(), value.trim());
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      aria-modal="true"
      role="dialog"
    >
      <div
        className="absolute inset-0 bg-neutral-900/40"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="relative w-full max-w-md rounded-2xl bg-neutral-100 p-6 shadow-xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700"
          aria-label="Close"
        >
          <span className="material-symbols-rounded text-[20px] leading-none">
            close
          </span>
        </button>

        <form onSubmit={handleSubmit}>
          <h2 className="mb-4 text-base font-semibold text-primary">
            {mode === "create" ? "New secret" : `Edit "${initialName}"`}
          </h2>

          {mode === "create" && (
            <div className="mb-4">
              <label
                htmlFor="secret-name"
                className="mb-1.5 block text-sm font-medium text-neutral-700"
              >
                Name
              </label>
              <input
                id="secret-name"
                ref={firstFieldRef}
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. STRIPE_API_KEY"
                pattern="[A-Za-z][A-Za-z0-9_]*"
                maxLength={100}
                required
                className="w-full rounded-xl border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary"
              />
              <p className="mt-1 text-xs text-neutral-400">
                Letters, digits, and underscores only. Referenced in a workflow
                as{" "}
                <code className="rounded bg-neutral-200 px-1">
                  {"{{secrets."}
                  {name || "NAME"}
                  {"}}"}
                </code>
                .
              </p>
            </div>
          )}

          <div className="mb-6">
            <label
              htmlFor="secret-value"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Value
            </label>
            <input
              id="secret-value"
              ref={mode === "edit" ? firstFieldRef : undefined}
              type="password"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={
                mode === "create"
                  ? "Paste the API key or token"
                  : "Enter the new value"
              }
              autoComplete="off"
              required
              className="w-full rounded-xl border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary"
            />
            <p className="mt-1 text-xs text-neutral-400">
              {mode === "create"
                ? "Stored encrypted. It won't be shown again — save your own copy if you need it elsewhere."
                : "This replaces it immediately and can't be undone."}
            </p>
          </div>

          {error && <p className="mb-4 text-sm text-red-text">{error}</p>}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 disabled:opacity-60"
            >
              Cancel
            </button>

            <LoadingButton
              type="submit"
              isLoading={isSaving}
              disabled={!name.trim() || !value.trim()}
              className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:opacity-60"
            >
              {mode === "create" ? "Create" : "Save"}
            </LoadingButton>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Individual secret row ───────────────────────────────────────────────────

function SecretRow({
  secret,
  onEdit,
  onDelete,
}: {
  secret: Secret;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <li className="flex items-center justify-between rounded-xl border border-neutral-200 bg-neutral-100 px-4 py-3">
      <div className="min-w-0">
        <code className="text-sm font-medium text-primary">{secret.name}</code>

        <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-neutral-400">
          <span>
            Created{" "}
            {new Date(secret.createdAt).toLocaleDateString(undefined, {
              year: "numeric",
              month: "short",
              day: "numeric",
            })}
          </span>

          {secret.updatedAt !== secret.createdAt && (
            <span>
              Updated{" "}
              {new Date(secret.updatedAt).toLocaleDateString(undefined, {
                year: "numeric",
                month: "short",
                day: "numeric",
              })}
            </span>
          )}
        </div>
      </div>

      <div className="ml-3 flex flex-shrink-0 items-center gap-1">
        <button
          type="button"
          onClick={onEdit}
          title="Edit value"
          className="rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-neutral-200 hover:text-neutral-700"
        >
          <span className="material-symbols-rounded text-[18px] leading-none">
            edit
          </span>
        </button>

        <button
          type="button"
          onClick={onDelete}
          title="Delete secret"
          className="rounded-lg p-1.5 text-neutral-400 transition-colors hover:bg-red-bg hover:text-red-text"
        >
          <span className="material-symbols-rounded text-[18px] leading-none">
            delete
          </span>
        </button>
      </div>
    </li>
  );
}
