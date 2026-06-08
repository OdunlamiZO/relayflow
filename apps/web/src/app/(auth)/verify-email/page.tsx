"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { verifyEmail } from "@/lib/authentication-api";
import { errorMessage } from "@/lib/error-message";

export default function VerifyEmailPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const rawReturnUrl = searchParams.get("returnUrl");
  const redirectTo =
    rawReturnUrl && rawReturnUrl.startsWith("/") ? rawReturnUrl : "/inbox";

  // Derive initial state from the token so we never call setState synchronously
  // inside an effect body (react-hooks/set-state-in-effect).
  const [status, setStatus] = useState<"verifying" | "success" | "error">(
    token ? "verifying" : "error"
  );
  const [error, setError] = useState<string | null>(
    token ? null : "No verification token found. Please check your email link."
  );

  // Prevent double-fire in React StrictMode
  const attempted = useRef(false);

  useEffect(() => {
    if (!token) return;
    if (attempted.current) return;
    attempted.current = true;

    verifyEmail(token)
      .then(() => {
        setStatus("success");
        router.push(redirectTo);
      })
      .catch((err: unknown) => {
        setError(errorMessage(err));
        setStatus("error");
      });
  }, [token, router, redirectTo]);

  if (status === "verifying") {
    return (
      <div className="flex flex-col items-center gap-4 py-4">
        <Spinner />
        <p className="text-sm text-neutral-500">Verifying your email…</p>
      </div>
    );
  }

  if (status === "success") {
    return (
      <div className="flex flex-col items-center gap-4 py-4">
        <Spinner />
        <p className="text-sm text-neutral-500">Verified! Redirecting…</p>
      </div>
    );
  }

  return (
    <>
      <div className="mb-4 flex items-center gap-2.5">
        <span
          className="material-symbols-rounded text-[26px] leading-none text-red-text"
          aria-hidden="true"
        >
          error
        </span>
        <h1 className="m-0 text-xl font-semibold text-primary">
          Verification failed
        </h1>
      </div>

      <p className="mb-6 text-sm text-neutral-600">{error}</p>

      <Link
        href="/signup"
        className="inline-flex items-center gap-1.5 text-sm font-semibold text-accent hover:underline"
      >
        <span
          className="material-symbols-rounded text-[15px] leading-none"
          aria-hidden="true"
        >
          arrow_back
        </span>
        Back to sign up
      </Link>
    </>
  );
}
