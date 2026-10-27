import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { errorMessage } from "@/lib/error-message";
import { messagingApi } from "@/lib/messaging-api";

type Payload = {
  name: string;
  botToken: string;
};

export function useConnectTelegram(workspaceId: string) {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: ({ name, botToken }: Payload) =>
      messagingApi.createChannelAccount({
        workspaceId,
        provider: "TELEGRAM",
        name,
        encryptedCredentials: botToken,
      }),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["channel-accounts", workspaceId],
      });
      showToast({ kind: "success", message: "Telegram bot connected." });
    },
  });
}
