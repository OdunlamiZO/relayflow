// Docs: docs/codebase-components.md#ai-agent-hooks
import { useQuery } from "@tanstack/react-query";

import { type LlmProviderOption, messagingApi } from "@/lib/messaging-api";

export function useLlmProviders(workspaceId: string) {
  return useQuery<LlmProviderOption[]>({
    queryKey: ["llm-providers", workspaceId],
    queryFn: () => messagingApi.listLlmProviders(workspaceId),
    enabled: !!workspaceId,
  });
}
