"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useDeleteWorkflow } from "@/hooks/use-delete-workflow";
import { useRenameWorkflow } from "@/hooks/use-rename-workflow";
import { useWorkflows } from "@/hooks/use-workflows";
import { errorMessage } from "@/lib/error-message";

type Props = {
  workspaceId: string;
};

export function WorkflowsList({ workspaceId }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const { data: workflows, isPending, isError } = useWorkflows(workspaceId);

  const renameWorkflow = useRenameWorkflow(workspaceId);
  const deleteWorkflow = useDeleteWorkflow(workspaceId);

  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<{
    id: string;
    name: string;
  } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const menuRef = useRef<HTMLDivElement>(null);

  // Close the open menu when clicking outside of it.
  useEffect(() => {
    if (!openMenuId) return;

    function handleClick(e: MouseEvent) {
      if (!menuRef.current?.contains(e.target as Node)) {
        setOpenMenuId(null);
      }
    }

    document.addEventListener("mousedown", handleClick);

    return () => document.removeEventListener("mousedown", handleClick);
  }, [openMenuId]);

  function startRename(id: string, currentName: string) {
    setOpenMenuId(null);
    setActionError(null);
    setRenamingId(id);
    setRenameValue(currentName);
  }

  function commitRename(id: string) {
    const trimmed = renameValue.trim();

    if (!trimmed) {
      setRenamingId(null);

      return;
    }

    renameWorkflow.mutate(
      { id, name: trimmed },
      {
        onSuccess: () => setRenamingId(null),
        onError: (err) => {
          setActionError(errorMessage(err));
          setRenamingId(null);
        },
      }
    );
  }

  function confirmDelete() {
    if (!deleteTarget) return;
    const { id } = deleteTarget;

    deleteWorkflow.mutate(id, {
      onSuccess: () => {
        setDeleteTarget(null);

        if (pathname === `/workflows/${id}`) {
          router.push(`/workflows?workspaceId=${workspaceId}`);
        }
      },
      onError: (err) => {
        setActionError(errorMessage(err));
        setDeleteTarget(null);
      },
    });
  }

  if (isPending) {
    return (
      <div className="flex flex-1 items-center justify-center py-8">
        <Spinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-1 items-center justify-center px-4 py-8">
        <p className="text-center text-sm text-red-text">
          Failed to load workflows.
        </p>
      </div>
    );
  }

  if (workflows.length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center px-4 py-8">
        <p className="text-center text-sm text-neutral-400">
          No workflows yet. Hit + to create one.
        </p>
      </div>
    );
  }

  return (
    <>
      {actionError && (
        <p className="px-4 pt-2 text-xs text-red-text">{actionError}</p>
      )}

      <ul className="flex-1 overflow-y-auto pb-14 md:pb-0">
        {workflows.map((workflow) => {
          const href = `/workflows/${workflow.id}?workspaceId=${workspaceId}`;
          const isActive = pathname === `/workflows/${workflow.id}`;
          const isRenaming = renamingId === workflow.id;

          return (
            <li key={workflow.id} className="group relative">
              {isRenaming ? (
                <div className="flex items-center gap-3 px-4 py-3">
                  <span
                    className={`h-2 w-2 flex-shrink-0 rounded-full ${
                      workflow.enabled ? "bg-green-border" : "bg-neutral-300"
                    }`}
                  />

                  <input
                    autoFocus
                    value={renameValue}
                    onChange={(e) => setRenameValue(e.target.value)}
                    onBlur={() => commitRename(workflow.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") commitRename(workflow.id);
                      if (e.key === "Escape") setRenamingId(null);
                    }}
                    className="flex-1 rounded-md border border-secondary bg-neutral-100 px-2 py-1 text-sm font-medium text-neutral-800 outline-none focus:ring-2 focus:ring-secondary/20"
                  />
                </div>
              ) : (
                <Link
                  href={href}
                  className={`flex items-center gap-3 px-4 py-3 pr-10 text-sm transition-colors hover:bg-neutral-200 ${
                    isActive ? "bg-neutral-200" : ""
                  }`}
                >
                  <span
                    className={`h-2 w-2 flex-shrink-0 rounded-full ${
                      workflow.enabled ? "bg-green-border" : "bg-neutral-300"
                    }`}
                  />

                  <span className="flex-1 truncate font-medium text-neutral-800">
                    {workflow.name}
                  </span>
                </Link>
              )}

              {!isRenaming && (
                <button
                  type="button"
                  onClick={() =>
                    setOpenMenuId((prev) =>
                      prev === workflow.id ? null : workflow.id
                    )
                  }
                  title="More options"
                  className={`absolute right-2 top-1/2 flex -translate-y-1/2 items-center justify-center rounded-md p-1 text-neutral-400 transition-colors hover:bg-neutral-300 hover:text-neutral-700 ${
                    openMenuId === workflow.id
                      ? "opacity-100"
                      : "opacity-100 md:opacity-0 md:group-hover:opacity-100 md:focus:opacity-100"
                  }`}
                >
                  <span
                    className="material-symbols-rounded text-[18px] leading-none"
                    aria-hidden="true"
                  >
                    more_vert
                  </span>
                </button>
              )}

              {openMenuId === workflow.id && (
                <div
                  ref={menuRef}
                  className="absolute right-2 top-10 z-10 w-36 overflow-hidden rounded-lg border border-neutral-300 bg-neutral-100 shadow-lg"
                >
                  <button
                    type="button"
                    onClick={() => startRename(workflow.id, workflow.name)}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-neutral-700 hover:bg-neutral-200"
                  >
                    <span
                      className="material-symbols-rounded text-[16px] leading-none"
                      aria-hidden="true"
                    >
                      edit
                    </span>
                    Rename
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setOpenMenuId(null);
                      setActionError(null);
                      setDeleteTarget({ id: workflow.id, name: workflow.name });
                    }}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-red-text hover:bg-red-bg"
                  >
                    <span
                      className="material-symbols-rounded text-[16px] leading-none"
                      aria-hidden="true"
                    >
                      delete
                    </span>
                    Delete
                  </button>
                </div>
              )}
            </li>
          );
        })}
      </ul>

      {deleteTarget && (
        <ConfirmModal
          title="Delete workflow?"
          description={
            <>
              <strong>{deleteTarget.name}</strong> will be permanently deleted.
              This can&apos;t be undone.
            </>
          }
          confirmLabel="Delete"
          destructive
          isPending={deleteWorkflow.isPending}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </>
  );
}
