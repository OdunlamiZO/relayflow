// Docs: docs/codebase-components.md#workspace-hooks
import { type WorkspaceMember } from "@/lib/messaging-api";

import { useWorkspaceMembers } from "./use-workspace-members";

export function useCurrentMember(
  workspaceId: string,
  userId: string | null | undefined
): WorkspaceMember | undefined {
  const { data: members } = useWorkspaceMembers(workspaceId);

  return members?.find((member) => member.userId === userId);
}
