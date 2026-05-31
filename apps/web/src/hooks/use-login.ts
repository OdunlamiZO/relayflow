import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { type LoginPayload, login as loginApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogin() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: LoginPayload) => loginApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: (data) => {
      // If 2FA is required the server hasn't established a session yet —
      // don't invalidate the auth query until after the OTP step succeeds.
      if (!data.twoFactorRequired) {
        void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      }
    },
  });
}
