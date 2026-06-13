import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useUpdateConversationAssignee(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      conversationId,
      assigneeId,
    }: {
      conversationId: string;
      assigneeId: string | null;
    }) =>
      messagingApi.updateConversationAssignee(workspaceId, conversationId, {
        assigneeId,
      }),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    },
  });
}
