import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type ChangePasswordPayload,
  changePassword as changePasswordApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useChangePassword() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: ChangePasswordPayload) => changePasswordApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: () => {
      showToast({ kind: "success", message: "Password changed successfully." });
    },
  });
}
