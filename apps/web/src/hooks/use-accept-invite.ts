import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useAcceptInvite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (token: string) => messagingApi.acceptInvite(token),

    onSuccess: () => {
      // Refresh workspace list so the new workspace appears immediately.
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
    },
  });
}
