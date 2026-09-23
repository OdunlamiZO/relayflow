import type { Metadata } from "next";
import Link from "next/link";

import { Footer } from "@/components/Footer";
import {
  CHANGELOG_RELEASES,
  CHANGE_CATEGORY_LABELS,
  CHANGE_CATEGORY_ORDER,
  formatReleaseDate,
} from "@/lib/changelog";

const GITHUB_REPOSITORY_URL = "https://github.com/OdunlamiZO/relayflow";

export const metadata: Metadata = {
  title: "Changelog — RelayFlow",
  description: "Release notes for RelayFlow",
};

export default function ChangelogPage() {
  return (
    <div className="min-h-screen bg-neutral-200 text-neutral-800">
      <header className="sticky top-0 z-10 border-b border-neutral-300 bg-neutral-100/95 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 sm:px-8">
          <Link
            href="/"
            className="flex items-center gap-2 text-sm font-bold tracking-tight text-accent"
          >
            <span
              className="material-symbols-rounded text-[20px]"
              aria-hidden="true"
            >
              account_tree
            </span>
            RelayFlow
          </Link>

          <Link
            href={GITHUB_REPOSITORY_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-lg bg-secondary px-3 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
          >
            GitHub
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-16 sm:px-8 sm:py-20">
        <h1 className="text-[1.9rem] font-bold leading-[1.15] tracking-tight text-primary sm:text-[2.5rem]">
          Changelog
        </h1>
        <p className="mt-5 text-base leading-7 text-neutral-600">
          Release notes for RelayFlow. For upgrade steps, see the{" "}
          <Link
            href="/docs/self-hosting"
            className="font-semibold text-accent hover:underline"
          >
            self-hosting guide
          </Link>
          .
        </p>

        <ol className="mt-12 flex flex-col gap-12">
          {CHANGELOG_RELEASES.map((release) => (
            <li
              key={release.version}
              id={`v${release.version}`}
              className="scroll-mt-24 border-t border-neutral-300 pt-8"
            >
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h2 className="m-0 text-xl font-bold tracking-tight text-primary">
                  <a
                    href={`#v${release.version}`}
                    className="hover:text-accent"
                  >
                    v{release.version}
                  </a>
                </h2>
                <time
                  dateTime={release.date}
                  className="text-sm text-neutral-500"
                >
                  {formatReleaseDate(release.date)}
                </time>
              </div>

              {CHANGE_CATEGORY_ORDER.map((category) => {
                const entries = release.changes[category];

                if (!entries?.length) {
                  return null;
                }

                return (
                  <section key={category} className="mt-6">
                    <h3 className="m-0 text-xs font-semibold uppercase tracking-wider text-neutral-500">
                      {CHANGE_CATEGORY_LABELS[category]}
                    </h3>
                    <ul className="mt-3 flex list-disc flex-col gap-2 pl-5 text-sm leading-6 text-neutral-600">
                      {entries.map((entry) => (
                        <li key={entry}>{entry}</li>
                      ))}
                    </ul>
                  </section>
                );
              })}

              <Link
                href={`${GITHUB_REPOSITORY_URL}/releases/tag/v${release.version}`}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-6 inline-block text-xs font-semibold text-neutral-600 transition-colors hover:text-accent"
              >
                Release on GitHub
              </Link>
            </li>
          ))}
        </ol>
      </main>

      <Footer />
    </div>
  );
}
