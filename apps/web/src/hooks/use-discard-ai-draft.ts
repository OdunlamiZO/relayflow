import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDiscardAiDraft(workspaceId: string, conversationId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => messagingApi.discardAiDraft(workspaceId, conversationId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-draft", workspaceId, conversationId],
      });
    },
  });
}
