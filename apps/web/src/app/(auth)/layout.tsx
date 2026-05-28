import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "RelayFlow",
};

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
