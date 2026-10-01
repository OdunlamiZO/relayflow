"use client";

import { useEffect, useRef, useState } from "react";

import { DesktopOnly } from "@/components/common/DesktopOnly";
import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";
import { useAuthentication } from "@/hooks/use-authentication";
import { useCurrentMember } from "@/hooks/use-current-member";

import { AiAgentPanel } from "./AiAgentPanel";
import { ChannelsList } from "./ChannelsList";
import { GeneralPanel } from "./GeneralPanel";
import { HooksPanel } from "./HooksPanel";
import { IntegrationsPanel } from "./IntegrationsPanel";
import { MembersList } from "./MembersList";

type Props = {
  workspaceId: string;
};

export function SettingsShell({ workspaceId }: Props) {
  const { user } = useAuthentication();
  const currentMember = useCurrentMember(workspaceId, user?.userId);

  // Owners always see channels. Members need CHANNELS_WRITE or CHANNELS_DELETE.
  // While currentMember is loading (undefined), default to showing channels so owners
  // don't see a flash of missing content.
  const canSeeChannels =
    !currentMember ||
    currentMember.role === "OWNER" ||
    currentMember.permissions.includes("CHANNELS_WRITE") ||
    currentMember.permissions.includes("CHANNELS_DELETE");

  const canManageAiAgent =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("AI_AGENT_WRITE") === true;

  const canManageApiKeys =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("API_KEYS_WRITE") === true;

  const canManageWebhook =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("WEBHOOKS_WRITE") === true;

  const canManageSecrets =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("SECRETS_WRITE") === true;

  const canManageHooks =
    currentMember?.role === "OWNER" ||
    currentMember?.permissions.includes("WORKFLOWS_WRITE") === true;

  const canSeeIntegrations =
    canManageApiKeys || canManageWebhook || canManageSecrets;

  const isOwner = currentMember?.role === "OWNER";

  const canRenameWorkspace = isOwner;

  const navItems = [
    ...(canRenameWorkspace
      ? [{ href: "#general", icon: "settings", label: "General" }]
      : []),
    ...(canSeeChannels
      ? [{ href: "#channels", icon: "hub", label: "Channels" }]
      : []),
    { href: "#members", icon: "group", label: "Members" },
    ...(canManageAiAgent
      ? [{ href: "#ai-agent", icon: "smart_toy", label: "AI Agent" }]
      : []),
    ...(canSeeIntegrations
      ? [{ href: "#integrations", icon: "api", label: "Integrations" }]
      : []),
    ...(canManageHooks
      ? [{ href: "#hooks", icon: "rule", label: "Hooks" }]
      : []),
  ];

  const [activeHref, setActiveHref] = useState<string | null>(null);

  const mainRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const syncWithHash = () => {
      setActiveHref(window.location.hash || null);
      mainRef.current?.scrollTo({ top: 0 });
    };

    syncWithHash();
    window.addEventListener("hashchange", syncWithHash);

    return () => window.removeEventListener("hashchange", syncWithHash);
  }, []);

  const currentHref =
    navItems.find((item) => item.href === activeHref)?.href ??
    navItems[0]?.href;

  const sectionClass = (href: string) => (href === currentHref ? "" : "hidden");

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
              className={`flex flex-shrink-0 items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                item.href === currentHref
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
                      className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                        item.href === currentHref
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

          <main ref={mainRef} className="flex-1 overflow-y-auto pb-14 md:pb-0">
            {canRenameWorkspace && (
              <div id="general" className={sectionClass("#general")}>
                <GeneralPanel workspaceId={workspaceId} />
              </div>
            )}

            {canSeeChannels && (
              <div id="channels" className={sectionClass("#channels")}>
                <ChannelsList workspaceId={workspaceId} />
              </div>
            )}

            <div id="members" className={sectionClass("#members")}>
              <MembersList
                workspaceId={workspaceId}
                currentUserId={user?.userId ?? undefined}
              />
            </div>

            {canManageAiAgent && (
              <div id="ai-agent" className={sectionClass("#ai-agent")}>
                <DesktopOnly
                  title="AI agent settings need a bigger screen"
                  message="Open RelayFlow on a computer to set up AI agents."
                >
                  <AiAgentPanel workspaceId={workspaceId} />
                </DesktopOnly>
              </div>
            )}

            {canSeeIntegrations && (
              <div id="integrations" className={sectionClass("#integrations")}>
                <DesktopOnly
                  title="Integrations need a bigger screen"
                  message="Open RelayFlow on a computer to manage API keys, webhooks, and secrets."
                >
                  <IntegrationsPanel
                    workspaceId={workspaceId}
                    canManageApiKeys={canManageApiKeys}
                    canManageWebhook={canManageWebhook}
                    canManageSecrets={canManageSecrets}
                  />
                </DesktopOnly>
              </div>
            )}

            {canManageHooks && (
              <div id="hooks" className={sectionClass("#hooks")}>
                <DesktopOnly
                  title="Hooks need a bigger screen"
                  message="Open RelayFlow on a computer to write and test hooks."
                >
                  <HooksPanel workspaceId={workspaceId} />
                </DesktopOnly>
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
