import { useMutation, useQueryClient } from "@tanstack/react-query";

import { type CreateInviteRequest, messagingApi } from "@/lib/messaging-api";

export function useCreateInvite(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateInviteRequest) =>
      messagingApi.createInvite(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["workspace-invites", workspaceId],
      });
    },
  });
}
