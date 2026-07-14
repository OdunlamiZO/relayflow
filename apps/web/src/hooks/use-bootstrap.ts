import { useMutation } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type BootstrapPayload,
  bootstrap as bootstrapApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useBootstrap() {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: BootstrapPayload) => bootstrapApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
  });
}
