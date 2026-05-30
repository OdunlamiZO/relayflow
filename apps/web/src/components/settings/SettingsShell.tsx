"use client";

import { WorkspaceNav } from "@/components/workspace/WorkspaceNav";

import { ChannelsList } from "./ChannelsList";

type Props = {
  workspaceId: string;
};

export function SettingsShell({ workspaceId }: Props) {
  return (
    <div className="flex h-full overflow-hidden">
      <WorkspaceNav workspaceId={workspaceId} />

      {/* Settings sub-navigation */}
      <aside className="hidden w-52 flex-shrink-0 flex-col border-r border-neutral-300 bg-neutral-100 lg:flex">
        <div className="border-b border-neutral-300 px-4 py-3">
          <span className="text-sm font-semibold text-neutral-800">
            Settings
          </span>
        </div>

        <nav className="flex-1 overflow-y-auto p-2">
          <ul className="flex flex-col gap-0.5">
            <li>
              <a
                href="#channels"
                className="flex items-center gap-2.5 rounded-lg bg-neutral-200 px-3 py-2 text-sm font-medium text-neutral-900"
              >
                <span
                  className="material-symbols-rounded text-[16px] leading-none"
                  aria-hidden="true"
                >
                  hub
                </span>
                Channels
              </a>
            </li>
          </ul>
        </nav>
      </aside>

      {/* Content */}
      <main className="flex-1 overflow-y-auto">
        <ChannelsList workspaceId={workspaceId} />
      </main>
    </div>
  );
}
