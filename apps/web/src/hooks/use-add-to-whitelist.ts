import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useAddToWhitelist(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (phoneNumbers: string[]) =>
      messagingApi.addToWhitelist(workspaceId, phoneNumbers),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["whitelist", workspaceId],
      });
    },
  });
}
