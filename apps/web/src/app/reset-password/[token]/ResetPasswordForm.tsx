"use client";

import Link from "next/link";
import { useState } from "react";

import { useResetPassword } from "@/hooks/use-reset-password";
import { errorMessage } from "@/lib/error-message";

type Props = {
  token: string;
};

export function ResetPasswordForm({ token }: Props) {
  const {
    mutate: resetPassword,
    isPending,
    isSuccess,
    error,
  } = useResetPassword();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mismatch, setMismatch] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      setMismatch(true);

      return;
    }

    setMismatch(false);
    resetPassword({ token, newPassword });
  }

  if (isSuccess) {
    return (
      <div className="flex flex-col items-center gap-3 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-bg">
          <span
            className="material-symbols-rounded text-[28px] text-green-text"
            aria-hidden="true"
          >
            check_circle
          </span>
        </div>

        <h1 className="text-xl font-semibold text-primary">Password updated</h1>

        <p className="text-sm text-neutral-600">
          Your password has been reset. You can now sign in with it.
        </p>

        <Link
          href="/login"
          className="mt-2 flex w-full items-center justify-center rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
        >
          Go to login
        </Link>
      </div>
    );
  }

  return (
    <>
      <h1 className="m-0 text-xl font-semibold text-primary">
        Set a new password
      </h1>

      <p className="mb-6 mt-1 text-sm text-neutral-600">
        Choose a new password for your RelayFlow account.
      </p>

      {error && (
        <p className="mb-4 rounded-lg bg-red-bg px-3 py-2 text-sm text-red-text">
          {errorMessage(error)}
        </p>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="newPassword"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            New password
          </label>

          <input
            id="newPassword"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              setMismatch(false);
            }}
            placeholder="••••••••"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="confirmPassword"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Confirm new password
          </label>

          <input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            required
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              setMismatch(false);
            }}
            placeholder="••••••••"
            className={`w-full rounded-lg border px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:ring-2 ${
              mismatch
                ? "border-red-text bg-red-bg focus:border-red-text focus:ring-red-text/20"
                : "border-neutral-300 bg-neutral-100 focus:border-secondary focus:ring-secondary/20"
            }`}
          />

          {mismatch && (
            <p className="text-xs text-red-text">Passwords do not match.</p>
          )}
        </div>

        <button
          type="submit"
          disabled={isPending}
          className="mt-1 flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isPending ? (
            <>
              <span
                className="material-symbols-rounded animate-spin text-[16px]"
                aria-hidden="true"
              >
                progress_activity
              </span>
              Updating…
            </>
          ) : (
            "Update password"
          )}
        </button>
      </form>
    </>
  );
}
