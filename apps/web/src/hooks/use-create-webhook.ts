import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type SaveWebhookRequest,
  type WebhookConfig,
  messagingApi,
} from "@/lib/messaging-api";

export function useCreateWebhook(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<WebhookConfig, Error, SaveWebhookRequest>({
    mutationFn: (request) => messagingApi.createWebhook(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["webhooks", workspaceId],
      });
    },
  });
}
