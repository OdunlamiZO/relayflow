import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useSetDefaultAiAgentConfiguration(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (configurationId: string) =>
      messagingApi.setDefaultAiAgentConfiguration(workspaceId, configurationId),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-agent-configurations", workspaceId],
      });
    },
  });
}
