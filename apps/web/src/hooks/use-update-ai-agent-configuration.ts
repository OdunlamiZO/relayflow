import { useMutation, useQueryClient } from "@tanstack/react-query";

import type {
  AiAgentConfiguration,
  UpdateAiAgentConfigurationRequest,
} from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useUpdateAiAgentConfiguration(
  workspaceId: string,
  configurationId: string
) {
  const queryClient = useQueryClient();

  return useMutation<
    AiAgentConfiguration,
    Error,
    UpdateAiAgentConfigurationRequest
  >({
    mutationFn: (request) =>
      messagingApi.updateAiAgentConfiguration(
        workspaceId,
        configurationId,
        request
      ),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-agent-configurations", workspaceId],
      });
    },
  });
}
