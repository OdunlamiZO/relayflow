import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type SaveWebhookRequest,
  type WebhookConfig,
  messagingApi,
} from "@/lib/messaging-api";

export function useUpdateWebhook(workspaceId: string, webhookId: string) {
  const queryClient = useQueryClient();

  return useMutation<WebhookConfig, Error, SaveWebhookRequest>({
    mutationFn: (request) =>
      messagingApi.updateWebhook(workspaceId, webhookId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["webhooks", workspaceId],
      });
    },
  });
}
