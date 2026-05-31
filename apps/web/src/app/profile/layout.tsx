import Link from "next/link";
import { redirect } from "next/navigation";

import { AuthenticationPanel } from "@/app/authentication-panel";
import {
  getServerAuthenticationStatus,
  requireAuthentication,
} from "@/lib/server-authentication";

export const metadata = {
  title: "Profile — RelayFlow",
};

type Props = {
  children: React.ReactNode;
};

export default async function ProfileLayout({ children }: Props) {
  await requireAuthentication();

  const { anonymous } = await getServerAuthenticationStatus();

  if (anonymous) {
    redirect("/inbox");
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-neutral-200">
      <header className="flex flex-shrink-0 items-center justify-between border-b border-neutral-300 bg-neutral-100 px-4 py-3 sm:px-6">
        <Link
          href="/inbox"
          className="flex items-center gap-2 text-sm font-bold tracking-tight text-accent hover:opacity-80"
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            account_tree
          </span>
          RelayFlow
        </Link>

        <AuthenticationPanel />
      </header>

      <div className="flex-1 overflow-auto">{children}</div>
    </div>
  );
}
