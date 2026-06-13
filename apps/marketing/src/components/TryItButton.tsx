"use client";

import { useState } from "react";

import { Spinner } from "@/components/Spinner";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const appBaseUrl =
  process.env.NEXT_PUBLIC_APP_BASE_URL ?? "http://localhost:3000";

type Props = {
  fullWidth?: boolean;
};

type GuestSessionResponse = {
  workspaceId: string;
  recoveryToken: string;
};

export function TryItButton({ fullWidth = false }: Props) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState(false);

  async function handleClick() {
    setIsPending(true);
    setError(false);

    try {
      const response = await fetch(`${apiBaseUrl}/auth/guest`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({}),
      });

      if (!response.ok) {
        throw new Error("Failed to create guest session");
      }

      const { workspaceId, recoveryToken } =
        (await response.json()) as GuestSessionResponse;

      // Cross-app navigation — the recovery token is handed off via the URL
      // since localStorage is per-origin and the app runs on a different
      // domain. The app reads it on load and stores it for guest recovery.
      const params = new URLSearchParams({
        workspaceId,
        recoveryToken,
      });

      window.location.href = `${appBaseUrl}/inbox?${params.toString()}`;
    } catch {
      setError(true);
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
        className={`flex items-center justify-center gap-2 rounded-lg border border-neutral-100/20 bg-neutral-100/10 px-5 py-2.5 text-sm font-semibold text-neutral-100 backdrop-blur-sm transition-colors hover:bg-neutral-100/20 disabled:cursor-not-allowed disabled:opacity-60 ${fullWidth ? "w-full sm:w-auto" : ""}`}
      >
        See it live
        {isPending ? (
          <Spinner
            size="sm"
            className="border-neutral-100/40 border-t-neutral-100"
          />
        ) : (
          <span
            className="material-symbols-rounded text-[15px]"
            aria-hidden="true"
          >
            arrow_forward
          </span>
        )}
      </button>

      {error && (
        <p className="text-xs text-red-300">
          Something went wrong. Please try again.
        </p>
      )}
    </div>
  );
}
