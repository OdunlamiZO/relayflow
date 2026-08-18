"use client";

import { useState } from "react";

import { Spinner } from "@/components/Spinner";

const GITHUB_USERNAME_PATTERN =
  /^[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){0,38}$/;

export function PricingForm() {
  const [email, setEmail] = useState("");
  const [githubUsername, setGithubUsername] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setErrorMessage(null);

    if (!GITHUB_USERNAME_PATTERN.test(githubUsername)) {
      setErrorMessage("Enter a valid GitHub username.");

      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, githubUsername }),
      });
      const body = (await response.json()) as {
        authorizationUrl?: string;
        error?: string;
      };

      if (!response.ok || !body.authorizationUrl) {
        setErrorMessage(
          body.error ?? "Something went wrong. Please try again."
        );
        setIsSubmitting(false);

        return;
      }

      window.location.href = body.authorizationUrl;
    } catch {
      setErrorMessage("Something went wrong. Please try again.");
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3">
      <input
        type="email"
        required
        placeholder="you@company.com"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-4 py-2.5 text-sm text-neutral-800 outline-none transition-colors focus:border-secondary"
      />

      <div>
        <input
          type="text"
          required
          placeholder="GitHub username"
          value={githubUsername}
          onChange={(event) => setGithubUsername(event.target.value)}
          className="w-full rounded-lg border border-neutral-300 bg-neutral-100 px-4 py-2.5 text-sm text-neutral-800 outline-none transition-colors focus:border-secondary"
        />
        <p className="mt-1.5 text-xs text-neutral-500">
          {/* FREE MODE — restore to "Needed to set up your access after payment." when re-enabling payment. */}
          Needed to set up your access.
        </p>
      </div>

      {errorMessage ? (
        <p className="text-xs text-red-text">{errorMessage}</p>
      ) : null}

      <button
        type="submit"
        disabled={isSubmitting}
        className="inline-flex items-center justify-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark disabled:opacity-60"
      >
        {isSubmitting ? <Spinner size="sm" /> : null}
        {/* FREE MODE — restore to "Buy license" when re-enabling payment. */}
        Get free access
      </button>
    </form>
  );
}
