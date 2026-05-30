"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Spinner } from "@/components/common/Spinner";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useCreateWorkflow } from "@/hooks/use-create-workflow";

import { WorkflowsList } from "./WorkflowsList";

type Props = {
  workspaceId: string;
};

export function WorkflowsShell({ workspaceId }: Props) {
  const router = useRouter();
  const { mutate: createWorkflow, isPending } = useCreateWorkflow();

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  // Focus the input when the form opens.
  useEffect(() => {
    if (showCreate) inputRef.current?.focus();
  }, [showCreate]);

  // ESC closes the form.
  useEffect(() => {
    if (!showCreate) return;

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setShowCreate(false);
        setName("");
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [showCreate]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = name.trim();

    if (!trimmed) return;

    createWorkflow(
      { workspaceId, name: trimmed },
      {
        onSuccess: (workflow) => {
          setShowCreate(false);
          setName("");
          router.push(`/workflows/${workflow.id}?workspaceId=${workspaceId}`);
        },
      }
    );
  }

  return (
    <div className="flex h-full overflow-hidden">
      <WorkspaceNav workspaceId={workspaceId} />

      {/* Sidebar — full-width on mobile */}
      <aside className="flex w-full flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 md:w-72 lg:w-80">
        <div className="flex items-center justify-between border-b border-neutral-300 px-4 py-3">
          <span className="text-sm font-semibold text-neutral-800">
            Workflows
          </span>

          <button
            onClick={() => {
              setShowCreate((prev) => !prev);
              setName("");
            }}
            title="New workflow"
            className={`flex items-center rounded-md p-1 transition-colors hover:bg-neutral-200 hover:text-neutral-700 ${
              showCreate ? "text-neutral-700" : "text-neutral-400"
            }`}
          >
            <span
              className="material-symbols-rounded text-[16px] leading-none"
              aria-hidden="true"
            >
              {showCreate ? "close" : "add"}
            </span>
          </button>
        </div>

        {/* Inline create form */}
        {showCreate && (
          <form
            onSubmit={handleSubmit}
            className="border-b border-neutral-300 p-3"
          >
            <input
              ref={inputRef}
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Workflow name"
              className="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            />

            <div className="mt-2 flex gap-2">
              <button
                type="button"
                onClick={() => {
                  setShowCreate(false);
                  setName("");
                }}
                className="flex-1 rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:bg-neutral-200"
              >
                Cancel
              </button>

              <button
                type="submit"
                disabled={isPending || !name.trim()}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-secondary-dark disabled:opacity-60"
              >
                Create
                {isPending && (
                  <Spinner
                    size="sm"
                    className="border-white/40 border-t-white"
                  />
                )}
              </button>
            </div>
          </form>
        )}

        <WorkflowsList workspaceId={workspaceId} />
      </aside>

      {/* Main — hidden on mobile (tap a workflow to open the editor) */}
      <main className="hidden flex-1 items-center justify-center md:flex">
        <EmptyState
          icon="account_tree"
          title="Select a workflow"
          description="Choose a workflow from the list, or create a new one."
        />
      </main>
    </div>
  );
}
