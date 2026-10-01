"use client";

import Link from "next/link";
import { type ReactNode, useSyncExternalStore } from "react";

const DESKTOP_QUERY = "(min-width: 768px)";

function subscribe(onChange: () => void) {
  const query = window.matchMedia(DESKTOP_QUERY);
  query.addEventListener("change", onChange);

  return () => query.removeEventListener("change", onChange);
}

type Props = {
  title: string;
  message: string;
  back?: { href: string; label: string };
  children: ReactNode;
};

export function DesktopOnly({ title, message, back, children }: Props) {
  const isDesktop = useSyncExternalStore(
    subscribe,
    () => window.matchMedia(DESKTOP_QUERY).matches,
    () => true
  );

  if (isDesktop) {
    return children;
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-2 px-6 py-16 text-center">
      <span
        className="material-symbols-rounded text-[40px] leading-none text-neutral-400"
        aria-hidden="true"
      >
        desktop_windows
      </span>
      <p className="mt-2 text-sm font-semibold text-neutral-800">{title}</p>
      <p className="max-w-xs text-sm text-neutral-500">{message}</p>
      {back && (
        <Link
          href={back.href}
          className="mt-3 text-sm font-medium text-secondary hover:text-secondary-dark"
        >
          {back.label}
        </Link>
      )}
    </div>
  );
}
