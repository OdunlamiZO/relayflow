"use client";

import { useEffect, useRef, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { IconButton } from "@/components/common/IconButton";
import { LoadingButton } from "@/components/common/LoadingButton";
import { Spinner } from "@/components/common/Spinner";
import { useBuiltInHooks } from "@/hooks/use-built-in-hooks";
import { useCreateHook } from "@/hooks/use-create-hook";
import { useDeleteHook } from "@/hooks/use-delete-hook";
import { useHooks } from "@/hooks/use-hooks";
import { useTestHook } from "@/hooks/use-test-hook";
import { useUpdateHook } from "@/hooks/use-update-hook";
import { errorMessage } from "@/lib/error-message";
import {
  type BuiltInHook,
  type Hook,
  type SaveHookRequest,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
};

type EditorTarget =
  | { mode: "create"; initial?: SaveHookRequest }
  | { mode: "edit"; hook: Hook };

export function HooksPanel({ workspaceId }: Props) {
  const { data: hooks, isLoading } = useHooks(workspaceId);
  const { data: builtInHooks } = useBuiltInHooks(workspaceId);
  const deleteHook = useDeleteHook(workspaceId);

  const [editorTarget, setEditorTarget] = useState<EditorTarget | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Hook | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  function handleDelete() {
    if (!deleteTarget) return;

    setDeleteError(null);
    deleteHook.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
      onError: (error) => {
        setDeleteTarget(null);
        setDeleteError(errorMessage(error));
      },
    });
  }

  function copyBuiltIn(hook: BuiltInHook) {
    setEditorTarget({
      mode: "create",
      initial: {
        name: `${hook.name} (copy)`,
        description: hook.description,
        expression: hook.expression,
        errorMessage: hook.errorMessage,
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">Hooks</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Hooks check and clean up a contact&apos;s reply to an open-ended Ask
          Question node, or a value an AI agent extracts, before it&apos;s
          saved. They&apos;re written in{" "}
          <a
            href="https://docs.camunda.io/docs/components/modeler/feel/what-is-feel/"
            target="_blank"
            rel="noopener noreferrer"
            className="font-medium text-secondary hover:underline"
          >
            FEEL
          </a>
          . The reply or value is available as{" "}
          <code className="rounded bg-neutral-200 px-1 text-[11px]">value</code>{" "}
          and, in workflows, run variables as{" "}
          <code className="rounded bg-neutral-200 px-1 text-[11px]">
            variables
          </code>
          , e.g.{" "}
          <code className="rounded bg-neutral-200 px-1 text-[11px]">
            variables.contact.name
          </code>
          .
        </p>
      </div>

      <section className="mb-10">
        <div className="mb-4 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span
              className="material-symbols-rounded text-[20px] leading-none text-neutral-500"
              aria-hidden="true"
            >
              rule
            </span>
            <h2 className="text-base font-semibold text-primary">
              Custom hooks
            </h2>
          </div>

          <button
            type="button"
            onClick={() => setEditorTarget({ mode: "create" })}
            className="flex items-center gap-1 rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary"
          >
            <span
              className="material-symbols-rounded text-[14px] leading-none"
              aria-hidden="true"
            >
              add
            </span>
            New hook
          </button>
        </div>

        {deleteError && (
          <p className="mb-4 text-sm text-red-text">{deleteError}</p>
        )}

        {isLoading ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : !hooks || hooks.length === 0 ? (
          <div className="rounded-xl border border-dashed border-neutral-300 px-4 py-6 text-center">
            <p className="text-sm text-neutral-400">
              No custom hooks yet. Start from scratch or copy a built-in below.
            </p>
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {hooks.map((hook) => (
              <HookRow
                key={hook.id}
                name={hook.name}
                description={hook.description}
                expression={hook.expression}
                actions={
                  <>
                    <IconButton
                      icon="edit"
                      label="Edit hook"
                      onClick={() => setEditorTarget({ mode: "edit", hook })}
                    />
                    <IconButton
                      icon="delete"
                      label="Delete hook"
                      destructive
                      onClick={() => {
                        setDeleteError(null);
                        setDeleteTarget(hook);
                      }}
                    />
                  </>
                }
              />
            ))}
          </ul>
        )}
      </section>

      <hr className="mb-10 border-neutral-200" />

      <section>
        <div className="mb-4 flex items-center gap-2">
          <span
            className="material-symbols-rounded text-[20px] leading-none text-neutral-500"
            aria-hidden="true"
          >
            inventory_2
          </span>
          <h2 className="text-base font-semibold text-primary">Built-in</h2>
        </div>

        <ul className="flex flex-col gap-2">
          {builtInHooks?.map((hook) => (
            <HookRow
              key={hook.key}
              name={hook.name}
              description={hook.description}
              expression={hook.expression}
              actions={
                <IconButton
                  icon="content_copy"
                  label="Copy into a custom hook"
                  onClick={() => copyBuiltIn(hook)}
                />
              }
            />
          ))}
        </ul>
      </section>

      {editorTarget?.mode === "create" && (
        <CreateHookModal
          workspaceId={workspaceId}
          initial={editorTarget.initial}
          onClose={() => setEditorTarget(null)}
        />
      )}

      {editorTarget?.mode === "edit" && (
        <EditHookModal
          workspaceId={workspaceId}
          hook={editorTarget.hook}
          onClose={() => setEditorTarget(null)}
        />
      )}

      {deleteTarget && (
        <ConfirmModal
          title={`Delete "${deleteTarget.name}"?`}
          description="Hooks that are still used by a workflow or AI agent can't be deleted. This cannot be undone."
          confirmLabel="Delete"
          destructive
          isPending={deleteHook.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

// ── Create / edit modals ────────────────────────────────────────────────────

function CreateHookModal({
  workspaceId,
  initial,
  onClose,
}: {
  workspaceId: string;
  initial?: SaveHookRequest;
  onClose: () => void;
}) {
  const createHook = useCreateHook(workspaceId);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(request: SaveHookRequest) {
    setError(null);
    createHook.mutate(request, {
      onSuccess: onClose,
      onError: (mutationError) => setError(errorMessage(mutationError)),
    });
  }

  return (
    <HookEditorModal
      workspaceId={workspaceId}
      title="New hook"
      submitLabel="Create"
      initial={initial}
      isSaving={createHook.isPending}
      error={error}
      onSubmit={handleSubmit}
      onClose={onClose}
    />
  );
}

function EditHookModal({
  workspaceId,
  hook,
  onClose,
}: {
  workspaceId: string;
  hook: Hook;
  onClose: () => void;
}) {
  const updateHook = useUpdateHook(workspaceId, hook.id);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(request: SaveHookRequest) {
    setError(null);
    updateHook.mutate(request, {
      onSuccess: onClose,
      onError: (mutationError) => setError(errorMessage(mutationError)),
    });
  }

  return (
    <HookEditorModal
      workspaceId={workspaceId}
      title={`Edit "${hook.name}"`}
      submitLabel="Save"
      initial={hook}
      notice="Changes apply immediately to every workflow and AI agent using this hook."
      isSaving={updateHook.isPending}
      error={error}
      onSubmit={handleSubmit}
      onClose={onClose}
    />
  );
}

// ── Editor modal ────────────────────────────────────────────────────────────

const INPUT_CLASS =
  "w-full rounded-xl border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-primary placeholder-neutral-400 outline-none focus:border-secondary focus:ring-1 focus:ring-secondary";

function HookEditorModal({
  workspaceId,
  title,
  submitLabel,
  initial,
  notice,
  isSaving,
  error,
  onSubmit,
  onClose,
}: {
  workspaceId: string;
  title: string;
  submitLabel: string;
  initial?: SaveHookRequest;
  notice?: string;
  isSaving: boolean;
  error: string | null;
  onSubmit: (request: SaveHookRequest) => void;
  onClose: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [expression, setExpression] = useState(initial?.expression ?? "");
  const [hookErrorMessage, setHookErrorMessage] = useState(
    initial?.errorMessage ?? ""
  );
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

  const canSubmit =
    name.trim() !== "" &&
    expression.trim() !== "" &&
    hookErrorMessage.trim() !== "";

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    if (!canSubmit) return;

    onSubmit({
      name: name.trim(),
      description: description.trim() || null,
      expression,
      errorMessage: hookErrorMessage.trim(),
    });
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

      <div className="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-neutral-100 p-6 shadow-xl">
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
          <h2 className="mb-4 text-base font-semibold text-primary">{title}</h2>

          <div className="mb-4">
            <label
              htmlFor="hook-name"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Name
            </label>
            <input
              id="hook-name"
              ref={firstFieldRef}
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. Order number"
              maxLength={100}
              required
              className={INPUT_CLASS}
            />
          </div>

          <div className="mb-4">
            <label
              htmlFor="hook-description"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Description{" "}
              <span className="font-normal text-neutral-400">(optional)</span>
            </label>
            <input
              id="hook-description"
              type="text"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={500}
              className={INPUT_CLASS}
            />
          </div>

          <div className="mb-4">
            <label
              htmlFor="hook-expression"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Expression
            </label>
            <textarea
              id="hook-expression"
              value={expression}
              onChange={(event) => setExpression(event.target.value)}
              placeholder={'matches(trim(value), "^ORD-\\\\d{4,}$")'}
              rows={4}
              maxLength={4000}
              spellCheck={false}
              required
              className={`${INPUT_CLASS} resize-y font-mono text-xs leading-5`}
            />
            <p className="mt-1 text-xs leading-5 text-neutral-400">
              <code>true</code> accepts the reply as-is; <code>false</code> or{" "}
              <code>null</code> rejects it; any other result is saved in its
              place. For full control return{" "}
              <code>{"{valid: true, value: …, variables: {…}}"}</code> — put{" "}
              <code>value</code> last, since each entry can see the ones before
              it.
            </p>
          </div>

          <div className="mb-4">
            <label
              htmlFor="hook-error-message"
              className="mb-1.5 block text-sm font-medium text-neutral-700"
            >
              Error message
            </label>
            <input
              id="hook-error-message"
              type="text"
              value={hookErrorMessage}
              onChange={(event) => setHookErrorMessage(event.target.value)}
              placeholder="e.g. Order numbers look like ORD-1234. Please try again."
              maxLength={500}
              required
              className={INPUT_CLASS}
            />
            <p className="mt-1 text-xs text-neutral-400">
              Sent to the contact when a value is rejected. An Ask Question node
              can override it; an AI agent works it into its reply.
            </p>
          </div>

          <HookTester
            workspaceId={workspaceId}
            expression={expression}
            fallbackErrorMessage={hookErrorMessage}
          />

          {notice && <p className="mb-4 text-xs text-neutral-500">{notice}</p>}

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
              disabled={!canSubmit}
              className="rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:opacity-90 disabled:opacity-60"
            >
              {submitLabel}
            </LoadingButton>
          </div>
        </form>
      </div>
    </div>
  );
}

function HookTester({
  workspaceId,
  expression,
  fallbackErrorMessage,
}: {
  workspaceId: string;
  expression: string;
  fallbackErrorMessage: string;
}) {
  const testHook = useTestHook(workspaceId);
  const [sampleReply, setSampleReply] = useState("");

  const outcome = testHook.data;

  function runTest() {
    if (!expression.trim()) return;

    testHook.mutate({ expression, value: sampleReply });
  }

  return (
    <div className="mb-4 rounded-xl border border-neutral-200 bg-neutral-200/40 p-3">
      <label
        htmlFor="hook-sample-reply"
        className="mb-1.5 block text-xs font-medium text-neutral-600"
      >
        Try it with a sample reply
      </label>

      <div className="flex gap-2">
        <input
          id="hook-sample-reply"
          type="text"
          value={sampleReply}
          onChange={(event) => setSampleReply(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              runTest();
            }
          }}
          placeholder="What a contact might type"
          className={INPUT_CLASS}
        />

        <LoadingButton
          type="button"
          isLoading={testHook.isPending}
          disabled={!expression.trim()}
          onClick={runTest}
          className="flex-shrink-0 rounded-lg border border-neutral-300 px-3 py-2 text-xs font-semibold text-neutral-700 transition-colors hover:border-secondary hover:text-secondary disabled:opacity-60"
        >
          Test
        </LoadingButton>
      </div>

      {testHook.isError && (
        <p className="mt-2 text-xs text-red-text">
          {errorMessage(testHook.error)}
        </p>
      )}

      {outcome && !testHook.isPending && (
        <div className="mt-2 text-xs leading-5">
          {outcome.status === "ACCEPTED" && (
            <>
              <p className="m-0 font-semibold text-green-text">Accepted</p>
              <p className="m-0 text-neutral-600">
                Saved as{" "}
                <code className="rounded bg-neutral-200 px-1">
                  {JSON.stringify(outcome.value)}
                </code>
              </p>
              {Object.keys(outcome.variables).length > 0 && (
                <p className="m-0 text-neutral-600">
                  Extra variables{" "}
                  <code className="rounded bg-neutral-200 px-1">
                    {JSON.stringify(outcome.variables)}
                  </code>
                </p>
              )}
            </>
          )}

          {outcome.status === "REJECTED" && (
            <>
              <p className="m-0 font-semibold text-red-text">Rejected</p>
              <p className="m-0 text-neutral-600">
                The contact would see: “
                {outcome.errorMessage ||
                  fallbackErrorMessage ||
                  "(your error message)"}
                ”
              </p>
            </>
          )}

          {outcome.status === "ERROR" && (
            <>
              <p className="m-0 font-semibold text-red-text">
                The expression couldn&apos;t run
              </p>
              <p className="m-0 break-words font-mono text-neutral-600">
                {outcome.errorMessage}
              </p>
            </>
          )}

          {outcome.warnings.length > 0 && (
            <ul className="m-0 mt-1 list-disc pl-4 text-yellow-text">
              {outcome.warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

// ── Rows ────────────────────────────────────────────────────────────────────

function HookRow({
  name,
  description,
  expression,
  actions,
}: {
  name: string;
  description: string | null;
  expression: string;
  actions: React.ReactNode;
}) {
  return (
    <li className="flex items-start justify-between rounded-xl border border-neutral-200 bg-neutral-100 px-4 py-3">
      <div className="min-w-0">
        <p className="m-0 text-sm font-medium text-primary">{name}</p>
        {description && (
          <p className="m-0 mt-0.5 text-xs text-neutral-500">{description}</p>
        )}
        <code className="mt-1.5 block truncate text-[11px] text-neutral-400">
          {expression}
        </code>
      </div>

      <div className="ml-3 flex flex-shrink-0 items-center gap-1">
        {actions}
      </div>
    </li>
  );
}
