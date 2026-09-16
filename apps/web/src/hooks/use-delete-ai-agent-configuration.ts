import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useDeleteAiAgentConfiguration(
  workspaceId: string,
  configurationId: string
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      messagingApi.deleteAiAgentConfiguration(workspaceId, configurationId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-agent-configurations", workspaceId],
      });
    },
  });
}
