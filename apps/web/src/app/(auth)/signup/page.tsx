"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import { Spinner } from "@/components/common/Spinner";
import { InviteStatusMessage } from "@/components/invite/InviteStatusMessage";
import { useInvitePreview } from "@/hooks/use-invite-preview";
import { useSignup } from "@/hooks/use-signup";
import { errorMessage } from "@/lib/error-message";

export default function SignupPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const { data: preview, isLoading } = useInvitePreview(token);
  const { mutate: signup, isPending, error } = useSignup();

  const [name, setName] = useState("");
  const [password, setPassword] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!token || !preview) return;

    signup(
      { name, email: preview.email, password, inviteToken: token },
      {
        onSuccess: (data) => {
          router.push(`/inbox?workspaceId=${data.workspaceId}`);
        },
      }
    );
  }

  if (!token) {
    return (
      <div className="text-center">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-neutral-400"
          aria-hidden="true"
        >
          mail_lock
        </span>
        <h1 className="text-lg font-semibold text-primary">
          You need an invite
        </h1>
        <p className="mt-2 text-sm text-neutral-500">
          Ask your workspace owner for an invite link to create an account.
        </p>
        <Link
          href="/login"
          className="mt-5 inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
        >
          Go to login
        </Link>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="text-center">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-neutral-400"
          aria-hidden="true"
        >
          block
        </span>
        <h1 className="text-lg font-semibold text-primary">Invite not found</h1>
        <p className="mt-2 text-sm text-neutral-500">
          This invite link doesn&apos;t exist or has already been revoked.
        </p>
      </div>
    );
  }

  if (preview.status !== "PENDING") {
    return <InviteStatusMessage status={preview.status} />;
  }

  return (
    <>
      <h1 className="m-0 text-xl font-semibold text-primary">
        Join {preview.workspaceName}
      </h1>

      <p className="mb-6 mt-1 text-sm text-neutral-600">
        <span className="font-medium text-neutral-700">
          {preview.inviterName}
        </span>{" "}
        invited you to collaborate in this workspace.
      </p>

      {error && (
        <p className="mb-4 rounded-lg bg-red-bg px-3 py-2 text-sm text-red-text">
          {errorMessage(error)}
        </p>
      )}

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
            value={preview.email}
            disabled
            className="w-full rounded-lg border border-neutral-300 bg-neutral-200 px-3 py-2.5 text-sm text-neutral-500 outline-none"
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
