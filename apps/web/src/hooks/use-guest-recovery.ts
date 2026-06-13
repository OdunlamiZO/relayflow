import { useEffect, useState } from "react";

import { useQueryClient } from "@tanstack/react-query";

import { recoverGuestSession } from "@/lib/authentication-api";

const RECOVERY_TOKEN_KEY = "guestRecoveryToken";

type RecoveryState =
  | { status: "pending" }
  | { status: "none" }
  | { status: "recovered"; workspaceId: string };

/**
 * On mount, checks for a stored guest recovery token (set by TryItButton when
 * a guest session is created) and attempts to re-establish the session if the
 * cookie session has expired or been lost.
 */
export function useGuestRecovery(): RecoveryState {
  const queryClient = useQueryClient();
  // Always start as "pending" so the server-rendered markup matches the
  // client's pre-hydration render — localStorage isn't available on the
  // server, so checking it in the initial state would cause a mismatch.
  const [state, setState] = useState<RecoveryState>({ status: "pending" });

  useEffect(() => {
    const token = localStorage.getItem(RECOVERY_TOKEN_KEY);

    if (!token) {
      // Transition out of the initial "pending" render once we know there's
      // nothing to recover. The initial state must be a constant (not based
      // on localStorage) so the client's pre-hydration render matches the
      // server-rendered markup.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setState({ status: "none" });

      return;
    }

    recoverGuestSession(token)
      .then(async ({ workspaceId, recoveryToken }) => {
        localStorage.setItem(RECOVERY_TOKEN_KEY, recoveryToken);
        await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
        setState({ status: "recovered", workspaceId });
      })
      .catch(() => {
        localStorage.removeItem(RECOVERY_TOKEN_KEY);
        setState({ status: "none" });
      });
  }, [queryClient]);

  return state;
}
