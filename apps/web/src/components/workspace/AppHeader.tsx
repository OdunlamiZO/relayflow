import Link from "next/link";
import { Suspense } from "react";

import { AuthenticationPanel } from "@/app/authentication-panel";

import { HeaderWorkspaceSwitcher } from "./HeaderWorkspaceSwitcher";

export function AppHeader() {
  return (
    <header className="flex flex-shrink-0 items-center justify-between gap-3 border-b border-neutral-300 bg-neutral-100 px-4 py-3 sm:px-6">
      <div className="flex min-w-0 items-center gap-2">
        <Link
          href="/inbox"
          className="flex flex-shrink-0 items-center gap-2 text-sm font-bold tracking-tight text-accent hover:opacity-80"
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            account_tree
          </span>
          RelayFlow
        </Link>

        <Suspense>
          <HeaderWorkspaceSwitcher />
        </Suspense>
      </div>

      <AuthenticationPanel />
    </header>
  );
}
