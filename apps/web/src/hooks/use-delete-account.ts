import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type DeleteAccountPayload,
  authenticationApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useDeleteAccount() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: DeleteAccountPayload) =>
      authenticationApi.deleteAccount(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}
