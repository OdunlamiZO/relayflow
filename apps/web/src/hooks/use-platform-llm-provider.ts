import { useQuery } from "@tanstack/react-query";

import { type LlmProvider, messagingApi } from "@/lib/messaging-api";

export function usePlatformLlmProvider(workspaceId: string) {
  return useQuery<LlmProvider>({
    queryKey: ["platform-llm-provider", workspaceId],
    queryFn: async () =>
      (await messagingApi.getPlatformLlmProvider(workspaceId)).provider,
    enabled: !!workspaceId,
  });
}
