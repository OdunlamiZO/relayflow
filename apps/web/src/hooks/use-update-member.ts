import { useMutation, useQueryClient } from "@tanstack/react-query";

import { type UpdateMemberRequest, messagingApi } from "@/lib/messaging-api";

export function useUpdateMember(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      memberId,
      request,
    }: {
      memberId: string;
      request: UpdateMemberRequest;
    }) => messagingApi.updateMember(workspaceId, memberId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["workspace-members", workspaceId],
      });
    },
  });
}
