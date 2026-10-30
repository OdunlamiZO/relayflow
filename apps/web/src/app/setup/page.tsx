"use client";

// Docs: docs/codebase-components.md#setup-page
import { useRouter } from "next/navigation";
import { useState } from "react";

import { LoadingButton } from "@/components/common/LoadingButton";
import { useBootstrap } from "@/hooks/use-bootstrap";

export default function SetupPage() {
  const router = useRouter();
  const { mutate: bootstrap, isPending } = useBootstrap();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [workspaceName, setWorkspaceName] = useState("");

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    bootstrap(
      { name, email, password, workspaceName },
      {
        onSuccess: () => {
          router.push("/inbox");
        },
      }
    );
  }

  return (
    <>
      <h1 className="m-0 text-xl font-semibold text-primary">
        Set up your instance
      </h1>

      <p className="mb-6 mt-1 text-sm text-neutral-600">
        Create the first admin account and your workspace — you won&apos;t see
        this screen again.
      </p>

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
            onChange={(event) => {
              setName(event.target.value);
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
            onChange={(event) => {
              setEmail(event.target.value);
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
            onChange={(event) => {
              setPassword(event.target.value);
            }}
            placeholder="At least 8 characters"
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="workspaceName"
            className="text-xs font-semibold uppercase tracking-wide text-neutral-600"
          >
            Workspace name
          </label>
          <input
            id="workspaceName"
            type="text"
            autoComplete="organization"
            required
            value={workspaceName}
            onChange={(event) => {
              setWorkspaceName(event.target.value);
            }}
            placeholder="Acme Inc."
            className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-3 py-2.5 text-sm text-neutral-800 outline-none placeholder:text-neutral-400 focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <LoadingButton
          type="submit"
          isLoading={isPending}
          className="mt-1 w-full rounded-lg bg-secondary px-4 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:cursor-not-allowed disabled:opacity-60"
        >
          Set up instance
        </LoadingButton>
      </form>
    </>
  );
}
