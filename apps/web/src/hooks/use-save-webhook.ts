import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  type SaveWebhookRequest,
  type WebhookConfig,
  messagingApi,
} from "@/lib/messaging-api";

export function useSaveWebhook(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<WebhookConfig, Error, SaveWebhookRequest>({
    mutationFn: (request) => messagingApi.saveWebhook(workspaceId, request),

    onSuccess: (data) => {
      queryClient.setQueryData(["workspace-webhook", workspaceId], data);
    },
  });
}
