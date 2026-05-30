import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteWebhook(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => messagingApi.deleteWebhook(workspaceId),

    onSuccess: () => {
      queryClient.setQueryData(["workspace-webhook", workspaceId], null);
    },
  });
}
