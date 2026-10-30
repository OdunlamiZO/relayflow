// Docs: docs/codebase-components.md#integration-hooks
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteWebhook(workspaceId: string, webhookId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => messagingApi.deleteWebhook(workspaceId, webhookId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["webhooks", workspaceId],
      });
    },
  });
}
