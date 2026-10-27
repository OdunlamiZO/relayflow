import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useChannelAiAgentAssignment(
  workspaceId: string,
  channelAccountId: string
) {
  return useQuery({
    queryKey: ["ai-agent-assignment", workspaceId, channelAccountId],
    queryFn: () =>
      messagingApi.getChannelAiAgentAssignment(workspaceId, channelAccountId),
    enabled: !!workspaceId && !!channelAccountId,
  });
}
