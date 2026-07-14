"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { useBootstrap } from "@/hooks/use-bootstrap";

export default function SetupPage() {
  const router = useRouter();
  const { mutate: bootstrap, isPending } = useBootstrap();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [workspaceName, setWorkspaceName] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

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
        Create the first admin account and your workspace. This only happens
        once.
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
            onChange={(e) => {
              setWorkspaceName(e.target.value);
            }}
            placeholder="Acme Inc."
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
              Setting up…
            </>
          ) : (
            "Set up instance"
          )}
        </button>
      </form>
    </>
  );
}
