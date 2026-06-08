"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

type Props = {
  workspaceId: string;
};

type NavItemProps = {
  href: string;
  icon: string;
  label: string;
  active: boolean;
};

function DesktopNavItem({ href, icon, label, active }: NavItemProps) {
  return (
    <div className="group relative">
      <Link
        href={href}
        className={`flex h-10 w-10 items-center justify-center rounded-lg transition-colors ${
          active
            ? "bg-neutral-200 text-neutral-900"
            : "text-neutral-400 hover:bg-neutral-200 hover:text-neutral-700"
        }`}
      >
        <span
          className="material-symbols-rounded text-[20px] leading-none"
          aria-hidden="true"
        >
          {icon}
        </span>
        <span className="sr-only">{label}</span>
      </Link>

      {/* Tooltip — appears to the right on hover */}
      <div
        role="tooltip"
        className="pointer-events-none absolute left-full top-1/2 z-50 ml-2 -translate-y-1/2 whitespace-nowrap rounded-lg border border-neutral-200 bg-neutral-100 px-2.5 py-1 text-xs font-medium text-neutral-700 shadow-sm opacity-0 transition-opacity group-hover:opacity-100"
      >
        {label}
      </div>
    </div>
  );
}

export function WorkspaceNav({ workspaceId }: Props) {
  const pathname = usePathname();

  const navItems: NavItemProps[] = [
    {
      href: `/inbox?workspaceId=${workspaceId}`,
      icon: "inbox",
      label: "Inbox",
      active:
        pathname.startsWith("/inbox") &&
        !pathname.startsWith("/inbox/channels"),
    },
    {
      href: `/contacts?workspaceId=${workspaceId}`,
      icon: "contacts",
      label: "Contacts",
      active: pathname.startsWith("/contacts"),
    },
    {
      href: `/workflows?workspaceId=${workspaceId}`,
      icon: "account_tree",
      label: "Workflows",
      active: pathname.startsWith("/workflows"),
    },
    {
      href: `/settings?workspaceId=${workspaceId}`,
      icon: "settings",
      label: "Settings",
      active: pathname.startsWith("/settings"),
    },
  ];

  return (
    <>
      {/* ── Desktop sidebar ─────────────────────────────────────────── */}
      <nav className="hidden w-14 flex-shrink-0 flex-col items-center justify-between border-r border-neutral-300 bg-neutral-100 py-3 md:flex">
        <div className="flex flex-col items-center gap-1">
          {navItems.slice(0, 3).map((item) => (
            <DesktopNavItem key={item.label} {...item} />
          ))}
        </div>

        <DesktopNavItem {...navItems[3]} />
      </nav>

      {/* ── Mobile bottom tab bar ───────────────────────────────────── */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 flex items-center border-t border-neutral-300 bg-neutral-100 md:hidden">
        {navItems.map((item) => (
          <Link
            key={item.label}
            href={item.href}
            className={`flex flex-1 flex-col items-center gap-0.5 py-2 transition-colors ${
              item.active
                ? "text-secondary"
                : "text-neutral-400 hover:text-neutral-700"
            }`}
          >
            <span
              className="material-symbols-rounded text-[22px] leading-none"
              aria-hidden="true"
            >
              {item.icon}
            </span>
            <span className="text-[10px] font-medium">{item.label}</span>
          </Link>
        ))}
      </nav>
    </>
  );
}
