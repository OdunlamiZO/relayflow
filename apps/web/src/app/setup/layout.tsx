import type { Metadata } from "next";
import Link from "next/link";

import { redirectIfBootstrapped } from "@/lib/server-authentication";

export const metadata: Metadata = {
  title: "Set up RelayFlow",
};

type Props = {
  children: React.ReactNode;
};

export default async function SetupLayout({ children }: Props) {
  // Once an instance has been set up, /setup has nothing left to do —
  // send visitors to /login instead.
  await redirectIfBootstrapped();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-neutral-200 px-4 py-16">
      <Link
        href="/"
        className="mb-10 flex items-center gap-2 text-lg font-bold text-accent transition-opacity hover:opacity-80"
      >
        <span
          className="material-symbols-rounded text-[22px]"
          aria-hidden="true"
        >
          account_tree
        </span>
        RelayFlow
      </Link>

      <div className="w-full max-w-[420px] rounded-2xl border border-neutral-300 bg-neutral-100 p-6 shadow-sm sm:p-8">
        {children}
      </div>
    </div>
  );
}
