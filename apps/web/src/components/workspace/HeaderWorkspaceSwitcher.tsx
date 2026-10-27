"use client";

import { useSearchParams } from "next/navigation";

import { WorkspaceSwitcher } from "./WorkspaceSwitcher";

export function HeaderWorkspaceSwitcher() {
  const workspaceId = useSearchParams().get("workspaceId");

  if (!workspaceId) {
    return null;
  }

  return (
    <>
      <span className="text-neutral-300" aria-hidden="true">
        /
      </span>
      <WorkspaceSwitcher workspaceId={workspaceId} />
    </>
  );
}
