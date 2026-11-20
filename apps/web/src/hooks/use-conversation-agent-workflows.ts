// Docs: docs/codebase-components.md#ai-agent-hooks
import { useQuery } from "@tanstack/react-query";

import { messagingApi } from "@/lib/messaging-api";

export function useConversationAgentWorkflows(
  workspaceId: string,
  conversationId: string
) {
  return useQuery({
    queryKey: ["agent-workflows", workspaceId, conversationId],
    queryFn: () =>
      messagingApi.getConversationAgentWorkflows(workspaceId, conversationId),
    enabled: !!workspaceId && !!conversationId,
  });
}
