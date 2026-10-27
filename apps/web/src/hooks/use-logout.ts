import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { authenticationApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogout() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: () => authenticationApi.logout(),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      // Drops the guest recovery token so the login page doesn't re-establish the anonymous session.
      localStorage.removeItem("guestRecoveryToken");

      queryClient.clear();
    },
  });
}
