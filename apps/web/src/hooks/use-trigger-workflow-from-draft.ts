import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useTriggerWorkflowFromDraft(
  workspaceId: string,
  conversationId: string
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (workflowId: string) =>
      messagingApi.triggerWorkflowFromDraft(
        workspaceId,
        conversationId,
        workflowId
      ),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-draft", workspaceId, conversationId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
    },
  });
}
