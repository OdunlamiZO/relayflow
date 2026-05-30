"use client";

import Link from "next/link";

import { useAuthentication } from "@/hooks/use-authentication";

export function GuestBanner() {
  const { isAnonymous } = useAuthentication();

  if (!isAnonymous) return null;

  return (
    <div className="flex flex-shrink-0 items-center justify-between border-b border-yellow-bg-hover bg-yellow-bg px-4 py-2.5">
      <p className="text-xs text-yellow-text-hover">
        <span className="font-semibold">Guest mode</span> — your data is
        temporary and will be removed after 24 hours.
      </p>

      <Link
        href="/signup"
        className="ml-4 flex-shrink-0 rounded-md bg-yellow-border-hover px-3 py-1 text-xs font-semibold text-white transition-colors hover:bg-yellow-text-hover"
      >
        Create account →
      </Link>
    </div>
  );
}
