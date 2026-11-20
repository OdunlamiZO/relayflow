"use client";

// Docs: docs/codebase-components.md#knowledgesourcespanel
import { useEffect, useRef, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { IconButton } from "@/components/common/IconButton";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useDeleteKnowledgeSource } from "@/hooks/use-delete-knowledge-source";
import { useKnowledgeSources } from "@/hooks/use-knowledge-sources";
import { useSaveKnowledgeSource } from "@/hooks/use-save-knowledge-source";
import { useTestKnowledgeSource } from "@/hooks/use-test-knowledge-source";
import { errorMessage } from "@/lib/error-message";
import {
  type KnowledgeSource,
  type KnowledgeSourceHeader,
} from "@/lib/messaging-api";

const DEFAULT_CACHE_SECONDS = 60;

const INPUT_CLASS =
  "w-full rounded-xl border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary";

type Props = {
  workspaceId: string;
};

type EditorTarget =
  | { mode: "create" }
  | { mode: "edit"; source: KnowledgeSource };

export function KnowledgeSourcesPanel({ workspaceId }: Props) {
  const { data: sources, isLoading } = useKnowledgeSources(workspaceId);
  const deleteSource = useDeleteKnowledgeSource(workspaceId);

  const [editorTarget, setEditorTarget] = useState<EditorTarget | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<KnowledgeSource | null>(
    null
  );
  const [deleteError, setDeleteError] = useState<string | null>(null);

  function handleDelete() {
    if (!deleteTarget) return;

    setDeleteError(null);
    deleteSource.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
      onError: (error) => {
        setDeleteTarget(null);
        setDeleteError(errorMessage(error));
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-primary">Knowledge sources</h1>
          <p className="mt-1.5 text-sm text-neutral-500">
            Live business data, like exchange rates, prices or stock levels, for
            an AI agent&apos;s knowledge base. Each source is a GET request that
            returns JSON. Put a value in a knowledge answer as{" "}
            <code className="rounded bg-neutral-200 px-1 text-[11px]">
              {"{{sources.NAME.field}}"}
            </code>
            , and the agent sees the current value when it replies.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setEditorTarget({ mode: "create" })}
          className="flex flex-shrink-0 items-center gap-1 rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary"
        >
          <span
            className="material-symbols-rounded text-[14px] leading-none"
            aria-hidden="true"
          >
            add
          </span>
          New source
        </button>
      </div>

      {deleteError && (
        <p className="mb-4 text-sm text-red-text" role="alert">
          {deleteError}
        </p>
      )}

      {isLoading ? (
        <div className="flex justify-center py-6">
          <Spinner />
        </div>
      ) : !sources || sources.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
          <p className="text-sm text-neutral-400">No knowledge sources yet.</p>
        </div>
      ) : (
        <ul className="flex flex-col gap-2">
          {sources.map((source) => (
            <li
              key={source.id}
              className="flex items-center justify-between rounded-xl border border-neutral-200 bg-neutral-100 px-4 py-3"
            >
              <div className="min-w-0">
                <code className="text-sm font-medium text-primary">
                  {source.name}
                </code>
                <p className="mt-0.5 truncate text-xs text-neutral-500">
                  {source.url}
                </p>
                <p className="mt-0.5 text-xs text-neutral-400">
                  {source.cacheSeconds === 0
                    ? "Fetched for every reply"
                    : `Fetched at most every ${source.cacheSeconds} seconds`}
                </p>
              </div>

              <div className="ml-3 flex flex-shrink-0 items-center gap-1">
                <IconButton
                  icon="edit"
                  label={`Edit ${source.name}`}
                  onClick={() => setEditorTarget({ mode: "edit", source })}
                />
                <IconButton
                  icon="delete"
                  label={`Delete ${source.name}`}
                  onClick={() => {
                    setDeleteError(null);
                    setDeleteTarget(source);
                  }}
                  destructive
                />
              </div>
            </li>
          ))}
        </ul>
      )}

      {editorTarget && (
        <KnowledgeSourceEditor
          workspaceId={workspaceId}
          source={editorTarget.mode === "edit" ? editorTarget.source : null}
          onClose={() => setEditorTarget(null)}
        />
      )}

      {deleteTarget && (
        <ConfirmModal
          title={`Delete "${deleteTarget.name}"?`}
          description="This can't be undone."
          confirmLabel="Delete"
          destructive
          isPending={deleteSource.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

function KnowledgeSourceEditor({
  workspaceId,
  source,
  onClose,
}: {
  workspaceId: string;
  source: KnowledgeSource | null;
  onClose: () => void;
}) {
  const saveSource = useSaveKnowledgeSource(workspaceId, source?.id);
  const testSource = useTestKnowledgeSource(workspaceId);

  const [name, setName] = useState(source?.name ?? "");
  const [url, setUrl] = useState(source?.url ?? "");
  const [headers, setHeaders] = useState<KnowledgeSourceHeader[]>(
    source?.headers ?? []
  );
  const [cacheSeconds, setCacheSeconds] = useState(
    String(source?.cacheSeconds ?? DEFAULT_CACHE_SECONDS)
  );
  const [error, setError] = useState<string | null>(null);
  const firstFieldRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    firstFieldRef.current?.focus();
  }, []);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }

    document.addEventListener("keydown", onKeyDown);

    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  const filledHeaders = headers.filter((header) => header.key.trim());

  function updateHeader(
    index: number,
    field: keyof KnowledgeSourceHeader,
    value: string
  ) {
    setHeaders(
      headers.map((header, headerIndex) =>
        headerIndex === index ? { ...header, [field]: value } : header
      )
    );
  }

  function handleTest() {
    testSource.mutate({ url: url.trim(), headers: filledHeaders });
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    if (!name.trim() || !url.trim()) return;

    setError(null);
    saveSource.mutate(
      {
        name: name.trim(),
        url: url.trim(),
        headers: filledHeaders,
        cacheSeconds: Number(cacheSeconds) || 0,
      },
      {
        onSuccess: onClose,
        onError: (saveError) => setError(errorMessage(saveError)),
      }
    );
  }

  const testResult = testSource.data;

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

      <div className="relative max-h-full w-full max-w-lg overflow-y-auto rounded-2xl bg-neutral-100 p-6 shadow-xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-neutral-400 hover:bg-neutral-200 hover:text-neutral-700"
          aria-label="Close"
        >
          <span className="material-symbols-rounded text-[20px] leading-none">
            close
          </span>
        </button>

        <form onSubmit={handleSubmit}>
          <h2 className="mb-4 text-base font-semibold text-primary">
            {source ? `Edit "${source.name}"` : "New knowledge source"}
          </h2>

          <div className="mb-4">
            <label
              htmlFor="knowledge-source-name"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Name
            </label>
            <input
              id="knowledge-source-name"
              ref={firstFieldRef}
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. rates"
              pattern="[A-Za-z][A-Za-z0-9_]*"
              maxLength={100}
              required
              className={INPUT_CLASS}
            />
            <p className="mt-1 text-xs text-neutral-400">
              Letters, digits and underscores. Used as{" "}
              <code className="rounded bg-neutral-200 px-1">
                {"{{sources."}
                {name || "NAME"}
                {".field}}"}
              </code>
              .
            </p>
          </div>

          <div className="mb-4">
            <label
              htmlFor="knowledge-source-url"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              URL
            </label>
            <input
              id="knowledge-source-url"
              type="text"
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              placeholder="https://api.example.com/rates"
              maxLength={2000}
              required
              className={INPUT_CLASS}
            />
          </div>

          <div className="mb-4">
            <span className="mb-1.5 block text-sm font-medium text-neutral-700">
              Headers
            </span>
            {headers.length > 0 && (
              <div className="mb-2 flex flex-col gap-2">
                {headers.map((header, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <input
                      type="text"
                      value={header.key}
                      onChange={(event) =>
                        updateHeader(index, "key", event.target.value)
                      }
                      placeholder="Authorization"
                      aria-label={`Header ${index + 1} name`}
                      className={INPUT_CLASS}
                    />
                    <input
                      type="text"
                      value={header.value}
                      onChange={(event) =>
                        updateHeader(index, "value", event.target.value)
                      }
                      placeholder="Bearer {{secrets.API_KEY}}"
                      aria-label={`Header ${index + 1} value`}
                      className={INPUT_CLASS}
                    />
                    <IconButton
                      icon="close"
                      label={`Remove header ${index + 1}`}
                      onClick={() =>
                        setHeaders(
                          headers.filter(
                            (_, headerIndex) => headerIndex !== index
                          )
                        )
                      }
                    />
                  </div>
                ))}
              </div>
            )}
            <button
              type="button"
              onClick={() => setHeaders([...headers, { key: "", value: "" }])}
              className="text-xs font-medium text-secondary hover:text-secondary-dark"
            >
              + Add header
            </button>
            <p className="mt-1 text-xs text-neutral-400">
              Put API keys in Integrations → Secrets and use them as{" "}
              <code className="rounded bg-neutral-200 px-1">
                {"{{secrets.NAME}}"}
              </code>{" "}
              in the URL or a header.
            </p>
          </div>

          <div className="mb-4">
            <label
              htmlFor="knowledge-source-cache"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Reuse a response for (seconds)
            </label>
            <input
              id="knowledge-source-cache"
              type="number"
              min={0}
              max={3600}
              value={cacheSeconds}
              onChange={(event) => setCacheSeconds(event.target.value)}
              className={`${INPUT_CLASS} max-w-32`}
            />
            <p className="mt-1 text-xs text-neutral-400">
              0 fetches it for every reply. Up to 3600.
            </p>
          </div>

          <div className="mb-6">
            <LoadingButton
              type="button"
              isLoading={testSource.isPending}
              disabled={!url.trim()}
              onClick={handleTest}
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 transition-colors hover:border-secondary hover:text-secondary disabled:opacity-60"
            >
              Test
            </LoadingButton>

            {testSource.error && (
              <p className="mt-2 text-xs text-red-text" role="alert">
                {errorMessage(testSource.error)}
              </p>
            )}

            {testResult && (
              <div className="mt-2">
                <p
                  className={`text-xs font-medium ${
                    testResult.error ? "text-red-text" : "text-green-text"
                  }`}
                >
                  {testResult.error ??
                    `HTTP ${testResult.statusCode}. Use a field below as {{sources.${
                      name || "NAME"
                    }.field}}.`}
                </p>
                {testResult.body && (
                  <pre className="mt-1.5 max-h-64 overflow-auto whitespace-pre-wrap break-all rounded-lg border border-neutral-300 bg-neutral-200 p-2 text-[11px] text-neutral-700">
                    {testResult.body}
                  </pre>
                )}
              </div>
            )}
          </div>

          {error && <p className="mb-4 text-sm text-red-text">{error}</p>}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={saveSource.isPending}
              className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-200 disabled:opacity-60"
            >
              Cancel
            </button>

            <LoadingButton
              type="submit"
              isLoading={saveSource.isPending}
              disabled={!name.trim() || !url.trim()}
              className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:opacity-60"
            >
              {source ? "Save" : "Create"}
            </LoadingButton>
          </div>
        </form>
      </div>
    </div>
  );
}
