import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useAiAgentConfiguration(workspaceId: string) {
  return useQuery({
    queryKey: ["ai-agent-configuration", workspaceId],
    queryFn: () => messagingApi.getAiAgentConfiguration(workspaceId),
    enabled: !!workspaceId,
  });
}
