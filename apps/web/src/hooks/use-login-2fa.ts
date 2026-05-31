import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type Login2FAPayload,
  login2FA as login2FAApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useLogin2FA() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: Login2FAPayload) => login2FAApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });
}
