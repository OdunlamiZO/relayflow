import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import { setup2FA as setup2FAApi } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useSetup2FA() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: setup2FAApi,
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
  });
}
