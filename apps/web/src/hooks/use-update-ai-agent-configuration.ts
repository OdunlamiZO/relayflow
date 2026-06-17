import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { UpdateAiAgentConfigurationRequest } from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useUpdateAiAgentConfiguration(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateAiAgentConfigurationRequest) =>
      messagingApi.updateAiAgentConfiguration(workspaceId, request),

    onSuccess: (updated) => {
      queryClient.setQueryData(
        ["ai-agent-configuration", workspaceId],
        updated
      );
    },
  });
}
