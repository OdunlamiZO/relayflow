import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useRemoveFromWhitelist(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (entryId: string) =>
      messagingApi.removeFromWhitelist(workspaceId, entryId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["whitelist", workspaceId],
      });
    },
  });
}
