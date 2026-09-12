import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useSendAiDraft(workspaceId: string, conversationId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (text?: string) =>
      messagingApi.sendAiDraft(workspaceId, conversationId, text),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-draft", workspaceId, conversationId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["messages", workspaceId, conversationId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    },
  });
}
