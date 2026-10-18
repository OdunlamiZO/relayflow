import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { type LoginPayload, authenticationApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogin() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: LoginPayload) => authenticationApi.login(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: (data) => {
      // With 2FA required there is no session yet, so the authentication query is invalidated after the OTP step.
      if (!data.twoFactorRequired) {
        void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      }
    },
  });
}
