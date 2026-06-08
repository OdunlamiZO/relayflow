"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

import { useSubscription } from "@/hooks/use-subscription";

function UpgradeBannerInner() {
  const searchParams = useSearchParams();
  const workspaceId = searchParams.get("workspaceId");

  const { data: subscription } = useSubscription(workspaceId ?? "");

  if (!workspaceId || !subscription?.upgradeRecommended) return null;

  return (
    <div className="flex flex-shrink-0 items-center justify-between border-b border-blue-bg-hover bg-blue-bg px-4 py-2.5">
      <p className="text-xs text-blue-text">
        <span className="font-semibold">You&apos;re on the Free plan</span> —
        upgrade to Pro for more channels, workflows, and team members.
      </p>

      <Link
        href={`/settings?workspaceId=${workspaceId}#billing`}
        className="ml-4 flex-shrink-0 rounded-md bg-blue-border-hover px-3 py-1 text-xs font-semibold text-white transition-colors hover:bg-blue-text-hover"
      >
        Upgrade →
      </Link>
    </div>
  );
}

export function UpgradeBanner() {
  return (
    <Suspense>
      <UpgradeBannerInner />
    </Suspense>
  );
}
