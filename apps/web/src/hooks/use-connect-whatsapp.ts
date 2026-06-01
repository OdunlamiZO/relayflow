import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { errorMessage } from "@/lib/error-message";
import { messagingApi } from "@/lib/messaging-api";

type Payload = {
  name: string;
  accessToken: string;
  phoneNumberId: string;
  verifyToken: string;
};

export function useConnectWhatsApp(workspaceId: string) {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: ({ name, accessToken, phoneNumberId, verifyToken }: Payload) =>
      messagingApi.createChannelAccount({
        workspaceId,
        provider: "WHATSAPP",
        name,
        // Credentials are serialised to JSON and encrypted server-side.
        encryptedCredentials: JSON.stringify({
          accessToken,
          phoneNumberId,
          verifyToken,
        }),
      }),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["channel-accounts", workspaceId],
      });
      showToast({ kind: "success", message: "WhatsApp account connected." });
    },
  });
}
