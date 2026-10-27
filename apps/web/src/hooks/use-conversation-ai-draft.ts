import { useQuery } from "@tanstack/react-query";

import { ApiError, messagingApi } from "@/lib/messaging-api";

export function useConversationAiDraft(
  workspaceId: string,
  conversationId: string | null
) {
  return useQuery({
    queryKey: ["ai-draft", workspaceId, conversationId],
    queryFn: async () => {
      try {
        return await messagingApi.getConversationAiDraft(
          workspaceId,
          conversationId!
        );
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
    enabled: !!workspaceId && !!conversationId,
    retry: false,
  });
}
