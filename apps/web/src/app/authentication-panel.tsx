"use client";

// Docs: docs/codebase-components.md#root-layout-components
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, useSyncExternalStore } from "react";

import { Spinner } from "@/components/common/Spinner";
import { useAuthentication } from "@/hooks/use-authentication";
import { useLogout } from "@/hooks/use-logout";

export function AuthenticationPanel() {
  const { user, isLoading, isAuthenticated } = useAuthentication();
  const { mutate: logout, isPending: isLoggingOut } = useLogout();
  const router = useRouter();

  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  function handleLogout() {
    logout(undefined, {
      onSuccess: () => {
        router.push("/login");
      },
    });
  }

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // Renders the skeleton until mounted, since the server has no cached authentication state and would mismatch on hydration.
  if (!mounted || isLoading) {
    return (
      <div className="h-10 w-10 animate-pulse rounded-full bg-neutral-300" />
    );
  }

  if (isAuthenticated && user) {
    const initial = (user.displayName ?? user.email ?? "?")
      .trim()
      .charAt(0)
      .toUpperCase();

    return (
      <div ref={containerRef} className="relative">
        <button
          type="button"
          onClick={() => setIsOpen((previous) => !previous)}
          aria-label="Account menu"
          aria-expanded={isOpen}
          className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-sm font-semibold text-neutral-100 transition-colors hover:bg-primary-dark focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          {user.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              alt=""
              src={user.avatarUrl}
              className="h-full w-full rounded-full object-cover"
            />
          ) : (
            <span>{initial}</span>
          )}
        </button>

        {isOpen && (
          <div className="absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-2xl border border-neutral-200 bg-neutral-100 shadow-xl">
            <div className="flex items-center gap-3 px-4 py-3.5">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-neutral-100">
                {user.avatarUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    alt=""
                    src={user.avatarUrl}
                    className="h-full w-full rounded-full object-cover"
                  />
                ) : (
                  <span>{initial}</span>
                )}
              </div>

              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-neutral-900">
                  {user.displayName ?? "User"}
                </p>

                {user.email && (
                  <p className="truncate text-xs text-neutral-400">
                    {user.email}
                  </p>
                )}
              </div>
            </div>

            <div className="border-t border-neutral-100" />

            <div className="p-1.5">
              <Link
                href="/profile"
                onClick={() => {
                  setIsOpen(false);
                }}
                className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:text-neutral-900"
              >
                <span
                  className="material-symbols-rounded text-[18px]"
                  aria-hidden="true"
                >
                  manage_accounts
                </span>
                Profile
              </Link>

              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  handleLogout();
                }}
                disabled={isLoggingOut}
                className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-neutral-600 transition-colors hover:text-red-text disabled:opacity-60"
              >
                <span
                  className="material-symbols-rounded text-[18px]"
                  aria-hidden="true"
                >
                  logout
                </span>
                Sign out
              </button>
            </div>
          </div>
        )}

        {isLoggingOut && (
          <div
            role="status"
            aria-label="Signing out"
            className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-100/80 backdrop-blur-sm"
          >
            <Spinner size="lg" />
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Link
        href="/login"
        className="rounded-lg bg-secondary px-3 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
      >
        Log in
      </Link>
    </div>
  );
}
