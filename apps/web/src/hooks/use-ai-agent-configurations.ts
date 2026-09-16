import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

/** All AI agent configs for a workspace — the default plus any per-channel overrides. */
export function useAiAgentConfigurations(workspaceId: string) {
  return useQuery({
    queryKey: ["ai-agent-configurations", workspaceId],
    queryFn: () => messagingApi.listAiAgentConfigurations(workspaceId),
    enabled: !!workspaceId,
  });
}
