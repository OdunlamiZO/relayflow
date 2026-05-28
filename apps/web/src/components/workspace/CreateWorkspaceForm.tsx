"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { useCreateWorkspace } from "@/hooks/use-create-workspace";

export function CreateWorkspaceForm() {
  const router = useRouter();
  const { mutate: createWorkspace, isPending } = useCreateWorkspace();

  const [name, setName] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    createWorkspace(
      { name },
      {
        onSuccess: (workspace) => {
          router.push(`/inbox?workspaceId=${workspace.id}`);
        },
      }
    );
  }

  return (
    <div className="flex h-full items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-2xl border border-blue-border bg-blue-bg">
          <span
            className="material-symbols-rounded text-[22px] text-blue-text"
            aria-hidden="true"
          >
            workspaces
          </span>
        </div>

        <h1 className="m-0 text-xl font-bold text-primary">
          Create your workspace
        </h1>

        <p className="mb-6 mt-2 text-sm text-neutral-600">
          A workspace holds your team&apos;s conversations, channels, and
          workflows.
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="workspace-name"
              className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
            >
              Workspace name
            </label>
            <input
              id="workspace-name"
              type="text"
              required
              autoFocus
              value={name}
              onChange={(e) => {
                setName(e.target.value);
              }}
              placeholder="Acme Support"
              className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            />
          </div>

          <button
            type="submit"
            disabled={isPending}
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isPending ? (
              <>
                <span
                  className="material-symbols-rounded animate-spin text-[15px]"
                  aria-hidden="true"
                >
                  progress_activity
                </span>
                Creating…
              </>
            ) : (
              <>
                Create workspace
                <span
                  className="material-symbols-rounded text-[15px]"
                  aria-hidden="true"
                >
                  arrow_forward
                </span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
