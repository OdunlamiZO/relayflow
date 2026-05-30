import { useQuery } from "@tanstack/react-query";

import {
  ApiError,
  type WebhookConfig,
  messagingApi,
} from "@/lib/messaging-api";

export function useWorkspaceWebhook(workspaceId: string) {
  return useQuery<WebhookConfig | null>({
    queryKey: ["workspace-webhook", workspaceId],
    queryFn: async () => {
      try {
        return await messagingApi.getWebhook(workspaceId);
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          return null;
        }

        throw err;
      }
    },
    enabled: !!workspaceId,
  });
}
