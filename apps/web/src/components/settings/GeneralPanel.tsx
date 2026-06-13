"use client";

import { useState } from "react";

import { useToast } from "@/components/providers/ToastProvider";
import { useUpdateWorkspace } from "@/hooks/use-update-workspace";
import { useWorkspace } from "@/hooks/use-workspaces";
import { errorMessage } from "@/lib/error-message";

type Props = {
  workspaceId: string;
};

export function GeneralPanel({ workspaceId }: Props) {
  const workspace = useWorkspace(workspaceId);
  const updateWorkspace = useUpdateWorkspace(workspaceId);
  const { showToast } = useToast();

  const [edited, setEdited] = useState<string | null>(null);
  const name = edited ?? workspace?.name ?? "";

  const trimmed = name.trim();
  const isUnchanged = trimmed === (workspace?.name ?? "");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!trimmed || isUnchanged) return;

    updateWorkspace.mutate(trimmed, {
      onSuccess: () => {
        showToast({ kind: "success", message: "Workspace renamed" });
      },
      onError: (error) => {
        showToast({ kind: "error", message: errorMessage(error) });
      },
    });
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">General</h1>
        <p className="mt-1.5 text-sm text-neutral-500">
          Manage your workspace&apos;s basic settings.
        </p>
      </div>

      <section>
        <h2 className="mb-3 text-xs font-semibold uppercase tracking-wide text-neutral-400">
          Workspace name
        </h2>

        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <input
            type="text"
            value={name}
            onChange={(e) => {
              setEdited(e.target.value);
            }}
            maxLength={160}
            required
            className="w-full flex-1 rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />

          <button
            type="submit"
            disabled={!trimmed || isUnchanged || updateWorkspace.isPending}
            className="flex-shrink-0 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            {updateWorkspace.isPending ? "Saving…" : "Save"}
          </button>
        </form>
      </section>
    </div>
  );
}
