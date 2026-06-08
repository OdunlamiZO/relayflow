"use client";

import { useEffect, useRef, useState } from "react";

import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useAuthentication } from "@/hooks/use-authentication";
import { useCurrentMember } from "@/hooks/use-current-member";
import { usePlans } from "@/hooks/use-plans";
import { useSubscription } from "@/hooks/use-subscription";

import { BillingPanel } from "./BillingPanel";
import { ChannelsList } from "./ChannelsList";
import { IntegrationsPanel } from "./IntegrationsPanel";
import { MembersList } from "./MembersList";

type Props = {
  workspaceId: string;
  isAnonymous: boolean;
};

export function SettingsShell({ workspaceId, isAnonymous }: Props) {
  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);

  // Owners always see channels. Members need CHANNELS_WRITE or CHANNELS_DELETE.
  // While currentMember is loading (undefined), default to showing channels so owners
  // don't see a flash of missing content.
  const canSeeChannels =
    isAnonymous ||
    !currentMember ||
    currentMember.role === "OWNER" ||
    currentMember.permissions.includes("CHANNELS_WRITE") ||
    currentMember.permissions.includes("CHANNELS_DELETE");

  const canManageApiKeys =
    !isAnonymous &&
    (currentMember?.role === "OWNER" ||
      currentMember?.permissions.includes("API_KEYS_WRITE") === true);

  const canManageWebhook =
    !isAnonymous &&
    (currentMember?.role === "OWNER" ||
      currentMember?.permissions.includes("WEBHOOKS_WRITE") === true);

  const canSeeIntegrations = canManageApiKeys || canManageWebhook;

  const isOwner = !isAnonymous && currentMember?.role === "OWNER";

  const { data: plans } = usePlans();
  const { data: subscription } = useSubscription(isOwner ? workspaceId : "");

  const hasPaidPlans = plans?.some((p) => p.upgradeAvailable) ?? false;
  const isOnPaidPlan =
    subscription?.plan !== "FREE" && subscription !== undefined;
  const showBilling = isOwner && (hasPaidPlans || isOnPaidPlan);

  const navItems = [
    ...(canSeeChannels
      ? [{ href: "#channels", icon: "hub", label: "Channels" }]
      : []),
    ...(!isAnonymous
      ? [{ href: "#members", icon: "group", label: "Members" }]
      : []),
    ...(canSeeIntegrations
      ? [{ href: "#integrations", icon: "api", label: "Integrations" }]
      : []),
    ...(showBilling
      ? [{ href: "#billing", icon: "payments", label: "Billing" }]
      : []),
  ];

  const [activeHref, setActiveHref] = useState<string>(
    navItems[0]?.href ?? "#channels"
  );

  const mainRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const main = mainRef.current;
    if (!main) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const topmost = entries
          .filter((e) => e.isIntersecting)
          .sort(
            (a, b) => a.boundingClientRect.top - b.boundingClientRect.top
          )[0];

        if (topmost) {
          const href = `#${topmost.target.id}`;
          setActiveHref(href);
          history.replaceState(null, "", href);
        }
      },
      { root: main, rootMargin: "0px 0px -60% 0px", threshold: 0 }
    );

    const ids = navItems.map((item) => item.href.slice(1));
    ids.forEach((id) => {
      const el = main.querySelector(`#${id}`);
      if (el) observer.observe(el);
    });

    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navItems.map((i) => i.href).join(",")]);

  return (
    <div className="flex h-full overflow-hidden">
      <WorkspaceNav workspaceId={workspaceId} />

      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile / tablet sub-nav — horizontal tab strip */}
        <nav className="flex flex-shrink-0 gap-1 overflow-x-auto border-b border-neutral-300 bg-neutral-100 p-2 lg:hidden">
          {navItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              onClick={() => setActiveHref(item.href)}
              className={`flex flex-shrink-0 items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                item.href === activeHref
                  ? "bg-neutral-200 text-neutral-900"
                  : "text-neutral-500 hover:bg-neutral-200 hover:text-neutral-900"
              }`}
            >
              <span
                className="material-symbols-rounded text-[15px] leading-none"
                aria-hidden="true"
              >
                {item.icon}
              </span>
              {item.label}
            </a>
          ))}
        </nav>

        <div className="flex flex-1 overflow-hidden">
          {/* Desktop vertical sidebar */}
          <aside className="hidden w-52 flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 lg:flex">
            <div className="border-b border-neutral-300 px-4 py-3">
              <span className="text-sm font-semibold text-neutral-800">
                Settings
              </span>
            </div>

            <nav className="flex-1 overflow-y-auto p-2">
              <ul className="flex flex-col gap-0.5">
                {navItems.map((item) => (
                  <li key={item.href}>
                    <a
                      href={item.href}
                      onClick={() => setActiveHref(item.href)}
                      className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                        item.href === activeHref
                          ? "bg-neutral-200 text-neutral-900"
                          : "text-neutral-500 hover:bg-neutral-200 hover:text-neutral-900"
                      }`}
                    >
                      <span
                        className="material-symbols-rounded text-[16px] leading-none"
                        aria-hidden="true"
                      >
                        {item.icon}
                      </span>
                      {item.label}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          </aside>

          {/* Content — sections scroll within this pane; anchor links jump to section ids */}
          <main
            ref={mainRef}
            className="flex-1 scroll-smooth overflow-y-auto pb-14 md:pb-0"
          >
            {canSeeChannels && (
              <div id="channels">
                <ChannelsList workspaceId={workspaceId} />
              </div>
            )}

            {!isAnonymous && (
              <div
                id="members"
                className={canSeeChannels ? "border-t border-neutral-200" : ""}
              >
                <MembersList
                  workspaceId={workspaceId}
                  currentUserId={user?.userId ?? undefined}
                />
              </div>
            )}

            {canSeeIntegrations && (
              <div
                id="integrations"
                className={
                  canSeeChannels || !isAnonymous
                    ? "border-t border-neutral-200"
                    : ""
                }
              >
                <IntegrationsPanel
                  workspaceId={workspaceId}
                  canManageApiKeys={canManageApiKeys}
                  canManageWebhook={canManageWebhook}
                />
              </div>
            )}

            {showBilling && (
              <div id="billing" className="border-t border-neutral-200">
                <BillingPanel workspaceId={workspaceId} isOwner={isOwner} />
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
