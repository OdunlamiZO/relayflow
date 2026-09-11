import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { type OtpPayload, authenticationApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useEnable2FA() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: OtpPayload) => authenticationApi.enable2FA(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["profile"] });
      showToast({
        kind: "success",
        message: "Two-factor authentication enabled.",
      });
    },
  });
}
