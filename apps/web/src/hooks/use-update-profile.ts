import { useMutation, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/providers/ToastProvider";
import {
  type UpdateProfilePayload,
  updateProfile as updateProfileApi,
} from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateProfileApi(payload),
    onError: (error) => {
      showToast({ kind: "error", message: errorMessage(error) });
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["profile"], data);
      showToast({ kind: "success", message: "Profile updated." });
    },
  });
}
