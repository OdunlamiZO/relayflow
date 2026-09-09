"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { LoadingButton } from "@/components/common/LoadingButton";
import { useCreateWorkspace } from "@/hooks/use-create-workspace";
import { useWorkspaces } from "@/hooks/use-workspaces";

type Props = {
  workspaceId: string;
};

export function WorkspaceSwitcher({ workspaceId }: Props) {
  const { data: workspaces = [] } = useWorkspaces();
  const { mutate: createWorkspace, isPending: isCreating } =
    useCreateWorkspace();
  const router = useRouter();

  const [isOpen, setIsOpen] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const current = workspaces.find((w) => w.id === workspaceId);

  function close() {
    setIsOpen(false);
    setShowCreate(false);
    setNewName("");
  }

  // Close on outside click.
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        close();
      }
    }

    document.addEventListener("mousedown", handleClickOutside);

    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // ESC: collapse create form first, then close dropdown.
  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        if (showCreate) {
          setShowCreate(false);
          setNewName("");
        } else {
          close();
        }
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, showCreate]);

  // Focus the name input when the create form opens.
  useEffect(() => {
    if (showCreate) inputRef.current?.focus();
  }, [showCreate]);

  function selectWorkspace(id: string) {
    close();

    if (id !== workspaceId) {
      router.push(`/inbox?workspaceId=${id}`);
    }
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    const name = newName.trim();

    if (!name) return;

    createWorkspace(
      { name },
      {
        onSuccess: (workspace) => {
          close();
          router.push(`/inbox?workspaceId=${workspace.id}`);
        },
      }
    );
  }

  return (
    <div
      ref={containerRef}
      className="relative flex min-w-0 flex-1 items-center"
    >
      <button
        type="button"
        onClick={() => {
          setIsOpen((prev) => !prev);
          setShowCreate(false);
          setNewName("");
        }}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label="Switch workspace"
        className="flex min-w-0 flex-1 items-center gap-1.5 rounded-md py-0.5 text-left transition-colors hover:bg-neutral-200"
      >
        <span
          className="material-symbols-rounded shrink-0 text-[16px] text-secondary"
          aria-hidden="true"
        >
          workspaces
        </span>

        <span className="flex-1 truncate text-sm font-semibold text-primary">
          {current?.name ?? "Inbox"}
        </span>

        <span
          className="material-symbols-rounded shrink-0 text-[14px] text-neutral-400"
          aria-hidden="true"
        >
          {isOpen ? "expand_less" : "expand_more"}
        </span>
      </button>

      {isOpen && (
        <div className="absolute left-0 top-full z-50 mt-1 w-64 overflow-hidden rounded-xl border border-neutral-200 bg-neutral-100 shadow-xl">
          {showCreate ? (
            <form onSubmit={handleCreate} className="p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-500">
                New workspace
              </p>

              <input
                ref={inputRef}
                type="text"
                required
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Workspace name"
                className="w-full rounded-lg border border-neutral-300 bg-neutral-50 px-3 py-2 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />

              <div className="mt-2.5 flex gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreate(false);
                    setNewName("");
                  }}
                  className="flex-1 rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-600 transition-colors hover:bg-neutral-100"
                >
                  Cancel
                </button>

                <LoadingButton
                  type="submit"
                  isLoading={isCreating}
                  disabled={!newName.trim()}
                  className="flex-1 rounded-lg bg-secondary px-3 py-1.5 text-xs font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:opacity-60"
                >
                  Create
                </LoadingButton>
              </div>
            </form>
          ) : (
            <>
              {workspaces.length > 0 && (
                <ul role="listbox" aria-label="Workspaces" className="p-1.5">
                  {workspaces.map((w) => (
                    <li key={w.id}>
                      <button
                        type="button"
                        role="option"
                        aria-selected={w.id === workspaceId}
                        onClick={() => selectWorkspace(w.id)}
                        className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-700 transition-colors hover:bg-neutral-100"
                      >
                        <span
                          className="material-symbols-rounded shrink-0 text-[15px] text-secondary"
                          aria-hidden="true"
                        >
                          workspaces
                        </span>

                        <span className="flex-1 truncate text-left">
                          {w.name}
                        </span>

                        {w.id === workspaceId && (
                          <span
                            className="material-symbols-rounded shrink-0 text-[15px] text-secondary"
                            aria-hidden="true"
                          >
                            check
                          </span>
                        )}
                      </button>
                    </li>
                  ))}
                </ul>
              )}

              <div
                className={
                  workspaces.length > 0
                    ? "border-t border-neutral-100 p-1.5"
                    : "p-1.5"
                }
              >
                <button
                  type="button"
                  onClick={() => setShowCreate(true)}
                  className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:bg-neutral-100"
                >
                  <span
                    className="material-symbols-rounded shrink-0 text-[15px]"
                    aria-hidden="true"
                  >
                    add
                  </span>
                  New workspace
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
