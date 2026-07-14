import Link from "next/link";

import { getServerAuthenticationStatus } from "@/lib/server-authentication";

import { InviteAcceptCard } from "./InviteAcceptCard";

export const metadata = {
  title: "Accept Invite — RelayFlow",
};

type Props = {
  searchParams: Promise<{ token?: string }>;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function fetchInvitePreview(token: string) {
  try {
    const res = await fetch(`${apiBaseUrl}/invites/${token}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return null;
    }

    return res.json();
  } catch {
    return null;
  }
}

export default async function InvitePage({ searchParams }: Props) {
  const { token } = await searchParams;

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

      <div className="w-full max-w-[460px]">
        <InviteContent token={token} />
      </div>
    </div>
  );
}

async function InviteContent({ token }: { token: string | undefined }) {
  if (!token) {
    return (
      <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-neutral-400"
          aria-hidden="true"
        >
          link_off
        </span>
        <h1 className="text-lg font-semibold text-primary">
          Invalid invite link
        </h1>
        <p className="mt-2 text-sm text-neutral-500">
          This invite link is missing or malformed.
        </p>
      </div>
    );
  }

  const [preview, authStatus] = await Promise.all([
    fetchInvitePreview(token),
    getServerAuthenticationStatus(),
  ]);

  if (!preview) {
    return (
      <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
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

  return (
    <InviteAcceptCard
      token={token}
      preview={preview}
      isAuthenticated={authStatus.authenticated}
    />
  );
}
