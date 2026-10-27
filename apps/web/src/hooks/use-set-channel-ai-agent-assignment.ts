import { useMutation, useQueryClient } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useSetChannelAiAgentAssignment(
  workspaceId: string,
  channelAccountId: string
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (configurationId: string | null) =>
      messagingApi.setChannelAiAgentAssignment(
        workspaceId,
        channelAccountId,
        configurationId
      ),

    onSuccess: (data) => {
      queryClient.setQueryData(
        ["ai-agent-assignment", workspaceId, channelAccountId],
        data
      );
    },
  });
}
