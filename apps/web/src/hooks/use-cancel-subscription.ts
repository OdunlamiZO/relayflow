import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

/**
 * Schedules cancellation of the workspace subscription.
 * The workspace keeps PRO access until the end of the current billing period.
 * Invalidates the subscription query so the UI reflects the new status immediately.
 */
export function useCancelSubscription(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => messagingApi.cancelSubscription(workspaceId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["subscription", workspaceId],
      });
    },
  });
}
