import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type OtpPayload,
  disable2FA as disable2FAApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useDisable2FA() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: OtpPayload) => disable2FAApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["profile"] });
      showToast({
        kind: "success",
        message: "Two-factor authentication disabled.",
      });
    },
  });
}
