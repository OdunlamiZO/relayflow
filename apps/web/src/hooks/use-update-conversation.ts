import { useMutation, useQueryClient } from "@tanstack/react-query";

import { type ConversationStatus, messagingApi } from "@/lib/messaging-api";

export function useUpdateConversation(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      conversationId,
      status,
    }: {
      conversationId: string;
      status: ConversationStatus;
    }) =>
      messagingApi.updateConversation(workspaceId, conversationId, { status }),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    },
  });
}
