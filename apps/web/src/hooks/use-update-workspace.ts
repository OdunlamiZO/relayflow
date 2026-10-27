import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useUpdateWorkspace(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (name: string) =>
      messagingApi.updateWorkspace(workspaceId, { name }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
    },
  });
}
