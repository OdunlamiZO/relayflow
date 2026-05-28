"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { useQueryClient } from "@tanstack/react-query";

import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { createGuestSession } from "@/lib/authentication-api";

type Props = {
  fullWidth?: boolean;
};

export function TryItButton({ fullWidth = false }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [isPending, setIsPending] = useState(false);

  async function handleClick() {
    setIsPending(true);

    try {
      const { workspaceId } = await createGuestSession();

      // Invalidate the auth cache so the inbox reads the new anonymous session
      // instead of the stale unauthenticated state (staleTime is 5 min).
      await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });

      router.push(`/inbox?workspaceId=${workspaceId}`);
    } catch {
      showToast({
        kind: "error",
        message: "Something went wrong. Please try again.",
      });
      setIsPending(false);
    }
  }

  return (
    <div
      className={`flex flex-col gap-2 ${fullWidth ? "w-full items-stretch sm:w-auto sm:items-start" : "items-start"}`}
    >
      <button
        onClick={handleClick}
        disabled={isPending}
        className={`flex items-center justify-center gap-2 rounded-lg border border-white/20 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur-sm transition-colors hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60 ${fullWidth ? "w-full sm:w-auto" : ""}`}
      >
        {isPending ? (
          <>
            Setting up…
            <Spinner size="sm" className="border-white/40 border-t-white" />
          </>
        ) : (
          <>
            See it live
            <span
              className="material-symbols-rounded text-[15px]"
              aria-hidden="true"
            >
              arrow_forward
            </span>
          </>
        )}
      </button>
    </div>
  );
}
