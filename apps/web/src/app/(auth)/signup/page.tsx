"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";

import { GoogleIcon } from "@/components/common/GoogleIcon";
import { useSignup } from "@/hooks/use-signup";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function SignupPage() {
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get("returnUrl") ?? undefined;

  const { mutate: signup, isPending } = useSignup();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [emailSent, setEmailSent] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    signup(
      { name, email, password, returnUrl },
      {
        onSuccess: () => {
          setEmailSent(true);
        },
      }
    );
  }

  if (emailSent) {
    return (
      <>
        <div className="mb-4 flex items-center gap-2.5">
          <span
            className="material-symbols-rounded text-[28px] leading-none text-green-text"
            aria-hidden="true"
          >
            mark_email_read
          </span>
          <h1 className="m-0 text-xl font-semibold text-primary">
            Check your inbox
          </h1>
        </div>

        <p className="mb-2 text-sm text-neutral-600">
          We sent a verification link to{" "}
          <span className="font-semibold text-primary">{email}</span>.
        </p>

        <p className="text-sm text-neutral-500">
          Click the link in the email to activate your account. If you
          don&apos;t see it, check your spam folder.
        </p>

        <p className="mt-6 text-center text-sm text-neutral-600">
          Already verified?{" "}
          <Link
            href="/login"
            className="font-semibold text-accent hover:underline"
          >
            Sign in
          </Link>
        </p>
      </>
    );
  }

  return (
    <>
      <h1 className="m-0 text-xl font-semibold text-primary">
        Create your account
      </h1>

      <p className="mb-6 mt-1 text-sm text-neutral-600">
        Start managing every customer conversation in one place
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
            htmlFor="name"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Full name
          </label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            required
            value={name}
            onChange={(e) => {
              setName(e.target.value);
            }}
            placeholder="Jane Smith"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

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
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
            }}
            placeholder="At least 8 characters"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
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
              Creating account…
            </>
          ) : (
            "Create account"
          )}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-neutral-600">
        Already have an account?{" "}
        <Link
          href="/login"
          className="font-semibold text-accent hover:underline"
        >
          Sign in
        </Link>
      </p>
    </>
  );
}
