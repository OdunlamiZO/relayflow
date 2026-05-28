import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { errorMessage } from "@/lib/error-message";
import { messagingApi } from "@/lib/messaging-api";

export function useDeleteChannelAccount(workspaceId: string) {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (channelAccountId: string) =>
      messagingApi.deleteChannelAccount(channelAccountId, workspaceId),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["channel-accounts", workspaceId],
      });
      showToast({ kind: "success", message: "Channel disconnected." });
    },
  });
}
