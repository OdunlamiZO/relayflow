import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useAiAgentConfigurations(workspaceId: string) {
  return useQuery({
    queryKey: ["ai-agent-configurations", workspaceId],
    queryFn: () => messagingApi.listAiAgentConfigurations(workspaceId),
    enabled: !!workspaceId,
  });
}
