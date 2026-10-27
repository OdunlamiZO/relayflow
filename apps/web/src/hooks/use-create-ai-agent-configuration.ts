import { useMutation, useQueryClient } from "@tanstack/react-query";

import type {
  AiAgentConfiguration,
  UpdateAiAgentConfigurationRequest,
} from "@/lib/messaging-api";
import { messagingApi } from "@/lib/messaging-api";

export function useCreateAiAgentConfiguration(workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<
    AiAgentConfiguration,
    Error,
    UpdateAiAgentConfigurationRequest
  >({
    mutationFn: (request) =>
      messagingApi.createAiAgentConfiguration(workspaceId, request),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["ai-agent-configurations", workspaceId],
      });
    },
  });
}
