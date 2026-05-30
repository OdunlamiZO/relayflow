"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import { GoogleIcon } from "@/components/common/GoogleIcon";
import { useLogin } from "@/hooks/use-login";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get("returnUrl");
  const { mutate: login, isPending } = useLogin();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    login(
      { email, password },
      {
        onSuccess: () => {
          // Redirect to returnUrl if it's a safe relative path, otherwise inbox.
          const destination =
            returnUrl && returnUrl.startsWith("/") ? returnUrl : "/inbox";
          router.push(destination);
        },
      }
    );
  }

  return (
    <>
      <h1 className="m-0 text-xl font-semibold text-primary">Welcome back</h1>

      <p className="mb-6 mt-1 text-sm text-neutral-600">
        Sign in to your RelayFlow account
      </p>

      <a
        href={`${apiBaseUrl}/oauth2/authorization/google`}
        className="flex w-full items-center justify-center gap-2.5 rounded-lg border border-neutral-300 bg-neutral-100 px-4 py-2.5 text-sm font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-200"
      >
        <GoogleIcon />
        Continue with Google
      </a>

      <div className="my-5 flex items-center gap-3">
        <div className="h-px flex-1 bg-neutral-300" />
        <span className="text-xs text-neutral-500">or</span>
        <div className="h-px flex-1 bg-neutral-300" />
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="email"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
            }}
            placeholder="you@company.com"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="password"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
            }}
            placeholder="••••••••"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <button
          type="submit"
          disabled={isPending}
          className="mt-1 flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isPending ? (
            <>
              <span
                className="material-symbols-rounded animate-spin text-[16px]"
                aria-hidden="true"
              >
                progress_activity
              </span>
              Signing in…
            </>
          ) : (
            "Sign in"
          )}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-neutral-600">
        Don&apos;t have an account?{" "}
        <Link
          href="/signup"
          className="font-semibold text-accent hover:underline"
        >
          Sign up
        </Link>
      </p>
    </>
  );
}
