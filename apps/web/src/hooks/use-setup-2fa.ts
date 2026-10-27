import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { authenticationApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useSetup2FA() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: () => authenticationApi.setup2FA(),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
  });
}
