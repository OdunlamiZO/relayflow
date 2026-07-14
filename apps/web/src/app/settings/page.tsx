import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { SettingsShell } from "@/components/settings/SettingsShell";

export const metadata = {
  title: "Settings — RelayFlow",
};

type Props = {
  searchParams: Promise<{ workspaceId?: string }>;
};

export default async function SettingsPage({ searchParams }: Props) {
  const { workspaceId } = await searchParams;

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
      <SettingsShell workspaceId={workspaceId} />
    </Suspense>
  );
}
