import Link from "next/link";

export function Footer() {
  return (
    <footer className="bg-neutral-100">
      <div className="mx-auto max-w-6xl px-6 py-8 sm:px-8">
        <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span className="flex items-center gap-2 text-sm font-bold text-accent">
            <span
              className="material-symbols-rounded text-[15px]"
              aria-hidden="true"
            >
              account_tree
            </span>
            RelayFlow
          </span>

          <div className="flex items-center gap-6">
            <Link
              href="/docs/self-hosting"
              className="text-xs font-semibold text-neutral-600 transition-colors hover:text-accent"
            >
              Setup docs
            </Link>

            <p className="m-0 text-xs text-neutral-500">
              &copy; {new Date().getFullYear()} RelayFlow &nbsp;&mdash;&nbsp;
              Built for developer-grade reliability.
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}
