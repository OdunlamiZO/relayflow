import { useMutation, useQueryClient } from "@tanstack/react-query";

import { type InviteMemberRequest, messagingApi } from "@/lib/messaging-api";

export function useInviteMember(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: InviteMemberRequest) =>
      messagingApi.inviteMember(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["workspace-members", workspaceId],
      });
    },
  });
}
