import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { errorMessage } from "@/lib/error-message";
import { type CreateMessageRequest, messagingApi } from "@/lib/messaging-api";

export function useSendMessage(workspaceId: string, conversationId: string) {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (request: CreateMessageRequest) =>
      messagingApi.createMessage(workspaceId, conversationId, request),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["messages", workspaceId, conversationId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["conversations", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["ai-draft", workspaceId, conversationId],
      });
    },
  });
}
