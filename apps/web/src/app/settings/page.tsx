import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { SettingsShell } from "@/components/settings/SettingsShell";
import { getServerAuthenticationStatus } from "@/lib/server-authentication";

export const metadata = {
  title: "Settings — RelayFlow",
};

type Props = {
  searchParams: Promise<{ workspaceId?: string }>;
};

export default async function SettingsPage({ searchParams }: Props) {
  const [{ workspaceId }, { anonymous }] = await Promise.all([
    searchParams,
    getServerAuthenticationStatus(),
  ]);

  if (!workspaceId) {
    redirect("/inbox");
  }

  return (
    <Suspense
      fallback={
        <div className="flex h-full items-center justify-center">
          <Spinner size="lg" />
        </div>
      }
    >
      <SettingsShell
        workspaceId={workspaceId}
        isAnonymous={anonymous ?? false}
      />
    </Suspense>
  );
}
