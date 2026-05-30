import { type WorkspaceMember } from "@/lib/messaging-api";

import { useWorkspaceMembers } from "./use-workspace-members";

export function useCurrentMember(
  workspaceId: string,
  userId: string | null | undefined
): WorkspaceMember | undefined {
  const { data: members } = useWorkspaceMembers(workspaceId);

  return members?.find((m) => m.userId === userId);
}
