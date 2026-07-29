import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { requestPasswordReset } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useRequestPasswordReset() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: () => requestPasswordReset(),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      showToast({
        kind: "success",
        message: "Password reset link sent — check your email.",
      });
    },
  });
}
