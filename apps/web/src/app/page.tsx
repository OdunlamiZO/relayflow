import Link from "next/link";
import { redirect } from "next/navigation";

import { TryItButton } from "@/components/landing/TryItButton";
import { getServerAuthenticationStatus } from "@/lib/server-authentication";

import { AuthenticationPanel } from "./authentication-panel";

// ── Data ───────────────────────────────────────────────────────────────────

const FEATURES = [
  {
    icon: "inbox",
    title: "Omnichannel inbox",
    description:
      "Every customer conversation in one unified workspace. Your team works from a single interface — customers reach you from any channel.",
    iconClass: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    icon: "account_tree",
    title: "Visual workflow automation",
    description:
      "Design message-driven automations with conditions, variable bindings, and HTTP integrations. Every step is inspectable.",
    iconClass: "border-purple-border bg-purple-bg text-purple-text",
  },
  {
    icon: "receipt_long",
    title: "Transparent execution logs",
    description:
      "Every workflow run is recorded end-to-end with full step-by-step visibility. Debug failures and audit behavior with confidence.",
    iconClass: "border-green-border bg-green-bg text-green-text",
  },
  {
    icon: "data_object",
    title: "Workflow variables",
    description:
      "Capture, transform, and pass data between nodes. Build context-aware automations that adapt to each conversation.",
    iconClass: "border-yellow-border bg-yellow-bg text-yellow-text",
  },
  {
    icon: "http",
    title: "Controllable HTTP steps",
    description:
      "Call any external API from within a workflow. Configure headers, body, and parse responses directly into your automation logic.",
    iconClass: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    icon: "supervisor_account",
    title: "Team collaboration",
    description:
      "Assign conversations, leave notes, and work as a team. Built for organizations that need visibility and accountability at scale.",
    iconClass: "border-purple-border bg-purple-bg text-purple-text",
  },
];

const PREVIEW_CONVOS = [
  {
    name: "Amara K.",
    channel: "Customer Support · TELEGRAM",
    time: "now",
    dot: "bg-secondary",
    active: true,
  },
  {
    name: "Dev Support",
    channel: "Sales Bot · WHATSAPP",
    time: "3m",
    dot: "bg-green-border",
    active: false,
  },
  {
    name: "Marcus L.",
    channel: "Help Desk · INSTAGRAM",
    time: "1h",
    dot: "bg-yellow-border",
    active: false,
  },
  {
    name: "Yuki T.",
    channel: "Customer Support · TELEGRAM",
    time: "2h",
    dot: "bg-neutral-400",
    active: false,
  },
];

// ── Page ───────────────────────────────────────────────────────────────────

export default async function Home() {
  const { authenticated } = await getServerAuthenticationStatus();

  if (authenticated) {
    redirect("/inbox");
  }

  return (
    <div className="min-h-screen bg-neutral-200 text-neutral-800">
      {/* ── Nav ─────────────────────────────────────────────────────── */}
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

          <AuthenticationPanel />
        </div>
      </header>

      <main>
        {/* ── Hero ────────────────────────────────────────────────────── */}
        <section className="border-b border-neutral-300 bg-neutral-100">
          <div className="mx-auto grid max-w-6xl grid-cols-1 gap-12 px-6 py-16 sm:px-8 sm:py-20 lg:grid-cols-2 lg:items-center lg:gap-16">
            {/* Left — copy */}
            <div>
              <h1 className="mt-5 text-[1.9rem] font-bold leading-[1.15] tracking-tight text-primary sm:text-[2.5rem] lg:text-5xl">
                All your customer conversations.{" "}
                <span className="text-secondary">One platform.</span>
              </h1>

              <p className="mt-5 max-w-lg text-base leading-7 text-neutral-600">
                Unified inbox, visual workflow automation, controllable HTTP
                steps, and transparent execution logs — built for teams who
                demand reliability.
              </p>

              <div className="mt-8 flex flex-wrap items-center gap-3">
                <Link
                  href="/signup"
                  className="inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
                >
                  Get started free
                  <span
                    className="material-symbols-rounded text-[15px]"
                    aria-hidden="true"
                  >
                    arrow_forward
                  </span>
                </Link>

                <Link
                  href="/inbox"
                  className="inline-flex items-center gap-2 rounded-lg border border-neutral-300 px-5 py-2.5 text-sm font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-200"
                >
                  <span
                    className="material-symbols-rounded text-[15px]"
                    aria-hidden="true"
                  >
                    inbox
                  </span>
                  Open inbox
                </Link>
              </div>

              <p className="mt-5 text-xs text-neutral-500">
                No credit card required &nbsp;·&nbsp; Works with any channel
              </p>
            </div>

            {/* Right — inbox preview */}
            <div className="flex justify-center lg:justify-end">
              <div className="w-full max-w-sm overflow-hidden rounded-2xl border border-neutral-300 bg-neutral-100 shadow-xl">
                {/* Browser chrome — desktop only */}
                <div className="hidden items-center gap-1.5 border-b border-neutral-300 bg-neutral-200 px-3.5 py-2.5 lg:flex">
                  <div className="h-2.5 w-2.5 rounded-full bg-red-border" />
                  <div className="h-2.5 w-2.5 rounded-full bg-yellow-border" />
                  <div className="h-2.5 w-2.5 rounded-full bg-green-border" />
                  <div className="ml-2 flex-1 rounded bg-neutral-300 px-3 py-1 text-[10px] text-neutral-500">
                    app.relayflow.io/inbox
                  </div>
                </div>

                {/* Inbox header */}
                <div className="flex items-center gap-2 border-b border-neutral-300 px-4 py-3">
                  <span
                    className="material-symbols-rounded text-[15px] text-secondary"
                    aria-hidden="true"
                  >
                    inbox
                  </span>
                  <span className="text-sm font-bold text-primary">Inbox</span>
                  <span className="ml-auto flex h-5 min-w-[20px] items-center justify-center rounded-full bg-secondary px-1.5 text-[10px] font-bold text-neutral-100">
                    4
                  </span>
                </div>

                {/* Conversations subheader — matches ConversationList */}
                <div className="flex items-center justify-between border-b border-neutral-300 px-4 py-3">
                  <h3 className="text-sm font-semibold text-primary">
                    Conversations
                  </h3>
                </div>

                {/* Conversation list — matches ConversationItem markup exactly */}
                <div className="divide-y divide-neutral-300">
                  {PREVIEW_CONVOS.map((conv) => (
                    <div
                      key={conv.name}
                      className={`w-full px-4 py-3 transition-colors hover:bg-neutral-200 ${
                        conv.active
                          ? "border-r-2 border-r-secondary bg-blue-bg"
                          : ""
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <span
                              className={`h-2 w-2 flex-shrink-0 rounded-full ${conv.dot}`}
                            />
                            <span className="truncate text-sm font-semibold text-neutral-800">
                              {conv.name}
                            </span>
                          </div>

                          <p className="mt-0.5 truncate text-xs text-neutral-500">
                            {conv.channel}
                          </p>
                        </div>

                        <span className="flex-shrink-0 text-xs text-neutral-500">
                          {conv.time}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Status bar */}
                <div className="flex items-center gap-2 border-t border-neutral-300 bg-neutral-200 px-4 py-2">
                  <span
                    className="material-symbols-rounded text-[13px] text-purple-text"
                    aria-hidden="true"
                  >
                    account_tree
                  </span>
                  <span className="text-xs text-neutral-600">
                    3 workflows active
                  </span>
                  <span className="ml-auto h-1.5 w-1.5 animate-pulse rounded-full bg-green-border" />
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ── Features ────────────────────────────────────────────────── */}
        <section className="border-b border-neutral-300 bg-neutral-200">
          <div className="mx-auto max-w-6xl px-6 py-16 sm:px-8 sm:py-20">
            <div className="mb-12">
              <h2 className="m-0 text-2xl font-bold tracking-tight text-primary sm:text-3xl">
                Everything your team needs
              </h2>
              <p className="mt-2 text-sm text-neutral-600">
                One platform, every tool — from first message to resolved.
              </p>
            </div>

            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map((f) => (
                <div
                  key={f.title}
                  className="flex flex-col gap-4 rounded-xl border border-neutral-300 bg-neutral-100 p-5 transition-all hover:border-neutral-400 hover:shadow-sm"
                >
                  <div
                    className={`flex h-9 w-9 items-center justify-center rounded-lg border ${f.iconClass}`}
                  >
                    <span
                      className="material-symbols-rounded text-[17px]"
                      aria-hidden="true"
                    >
                      {f.icon}
                    </span>
                  </div>

                  <div>
                    <h3 className="m-0 text-sm font-semibold text-primary">
                      {f.title}
                    </h3>
                    <p className="mt-1.5 text-sm leading-6 text-neutral-600">
                      {f.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Workflow ─────────────────────────────────────────────────── */}
        <section className="border-b border-neutral-300 bg-neutral-100">
          <div className="mx-auto grid max-w-6xl grid-cols-1 gap-10 px-6 py-16 sm:px-8 sm:py-20 lg:grid-cols-2 lg:items-center lg:gap-16">
            {/* Left — text */}
            <div>
              <h2 className="m-0 text-2xl font-bold tracking-tight text-primary sm:text-3xl">
                Automate any conversation pattern
              </h2>
              <p className="mt-3 text-sm leading-7 text-neutral-600">
                Build message-driven automations with a visual node editor.
                Trigger on any inbound message, branch on replies or data
                conditions, call external APIs, and send responses — no code
                required.
              </p>
              <p className="mt-4 text-sm leading-7 text-neutral-600">
                Every run is recorded end-to-end. Inspect inputs, outputs, and
                timing for each step so you can debug and iterate with
                confidence.
              </p>

              <div className="mt-6">
                <Link
                  href="/signup"
                  className="inline-flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-200"
                >
                  <span
                    className="material-symbols-rounded text-[15px]"
                    aria-hidden="true"
                  >
                    account_tree
                  </span>
                  Explore workflows
                  <span
                    className="material-symbols-rounded text-[13px]"
                    aria-hidden="true"
                  >
                    arrow_forward
                  </span>
                </Link>
              </div>
            </div>

            {/* Right — sample workflow diagram */}
            <div className="flex justify-center lg:justify-end">
              {/*
               * SVG connectors use:
               *   preserveAspectRatio="none"  — stretches to container width
               *   vectorEffect="non-scaling-stroke" — keeps stroke 1.5 px regardless
               * Coordinate space: viewBox "0 0 100 H" where 50 = centre,
               * 25 = left-branch centre, 75 = right-branch centre.
               */}
              <div
                role="img"
                aria-label="Sample support workflow: trigger, ask question, condition splits to HTTP Request or Send Message, both merge into End Conversation"
                className="w-full max-w-xs"
              >
                {/* ── Trigger ───────────────────────────────────────────── */}
                <div className="flex items-center gap-2.5 rounded-lg border border-blue-border bg-blue-bg px-4 py-2.5">
                  <span
                    className="material-symbols-rounded flex-shrink-0 text-[15px] text-blue-text"
                    aria-hidden="true"
                  >
                    bolt
                  </span>
                  <div>
                    <p className="whitespace-nowrap text-xs font-semibold text-blue-text">
                      Trigger
                    </p>
                    <p className="whitespace-nowrap text-[10px] text-neutral-500">
                      New inbound message
                    </p>
                  </div>
                </div>

                {/* straight connector */}
                <div className="mx-auto h-4 w-px bg-neutral-300" />

                {/* ── Ask Question ──────────────────────────────────────── */}
                <div className="flex items-center gap-2.5 rounded-lg border border-blue-border bg-blue-bg px-4 py-2.5">
                  <span
                    className="material-symbols-rounded flex-shrink-0 text-[15px] text-blue-text"
                    aria-hidden="true"
                  >
                    mark_unread_chat_alt
                  </span>
                  <div>
                    <p className="whitespace-nowrap text-xs font-semibold text-blue-text">
                      Ask Question
                    </p>
                    <p className="whitespace-nowrap text-[10px] text-neutral-500">
                      &quot;How can we help?&quot;
                    </p>
                  </div>
                </div>

                {/* straight connector */}
                <div className="mx-auto h-4 w-px bg-neutral-300" />

                {/* ── Condition ─────────────────────────────────────────── */}
                <div className="flex items-center gap-2.5 rounded-lg border border-yellow-border bg-yellow-bg px-4 py-2.5">
                  <span
                    className="material-symbols-rounded flex-shrink-0 text-[15px] text-yellow-text"
                    aria-hidden="true"
                  >
                    call_split
                  </span>
                  <div>
                    <p className="whitespace-nowrap text-xs font-semibold text-yellow-text">
                      Condition
                    </p>
                    <p className="whitespace-nowrap text-[10px] text-neutral-500">
                      Reply = &quot;urgent&quot;?
                    </p>
                  </div>
                </div>

                {/* split: centre → left-branch & right-branch */}
                <svg
                  viewBox="0 0 100 28"
                  preserveAspectRatio="none"
                  className="h-7 w-full"
                  aria-hidden="true"
                >
                  <path
                    d="M50,0 C50,14 25,14 25,28"
                    stroke="#E8EAED"
                    strokeWidth="1.5"
                    fill="none"
                    vectorEffect="non-scaling-stroke"
                  />
                  <path
                    d="M50,0 C50,14 75,14 75,28"
                    stroke="#E8EAED"
                    strokeWidth="1.5"
                    fill="none"
                    vectorEffect="non-scaling-stroke"
                  />
                </svg>

                {/* Yes / No badge row */}
                <div className="flex gap-4">
                  <div className="flex flex-1 justify-center">
                    <span className="rounded border border-green-border bg-green-bg px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide text-green-text">
                      Yes
                    </span>
                  </div>
                  <div className="flex flex-1 justify-center">
                    <span className="rounded border border-neutral-300 bg-neutral-200 px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide text-neutral-500">
                      No
                    </span>
                  </div>
                </div>

                {/* short connectors: badges → branch nodes */}
                <div className="flex gap-4">
                  <div className="flex flex-1 justify-center py-1">
                    <div className="h-3 w-px bg-neutral-300" />
                  </div>
                  <div className="flex flex-1 justify-center py-1">
                    <div className="h-3 w-px bg-neutral-300" />
                  </div>
                </div>

                {/* ── Branch nodes ──────────────────────────────────────── */}
                <div className="flex gap-4">
                  {/* Yes → HTTP Request */}
                  <div className="flex flex-1 items-center gap-2 rounded-lg border border-teal-border bg-teal-bg px-3 py-2.5">
                    <span
                      className="material-symbols-rounded flex-shrink-0 text-[13px] text-teal-text"
                      aria-hidden="true"
                    >
                      http
                    </span>
                    <div>
                      <p className="whitespace-nowrap text-[11px] font-semibold leading-tight text-teal-text">
                        HTTP Request
                      </p>
                      <p className="whitespace-nowrap text-[9px] leading-tight text-neutral-500">
                        Create ticket
                      </p>
                    </div>
                  </div>

                  {/* No → Send Message */}
                  <div className="flex flex-1 items-center gap-2 rounded-lg border border-green-border bg-green-bg px-3 py-2.5">
                    <span
                      className="material-symbols-rounded flex-shrink-0 text-[13px] text-green-text"
                      aria-hidden="true"
                    >
                      send
                    </span>
                    <div>
                      <p className="whitespace-nowrap text-[11px] font-semibold leading-tight text-green-text">
                        Send Message
                      </p>
                      <p className="whitespace-nowrap text-[9px] leading-tight text-neutral-500">
                        Queue response
                      </p>
                    </div>
                  </div>
                </div>

                {/* merge: left-branch & right-branch → centre */}
                <svg
                  viewBox="0 0 100 28"
                  preserveAspectRatio="none"
                  className="h-7 w-full"
                  aria-hidden="true"
                >
                  <path
                    d="M25,0 C25,14 50,14 50,28"
                    stroke="#E8EAED"
                    strokeWidth="1.5"
                    fill="none"
                    vectorEffect="non-scaling-stroke"
                  />
                  <path
                    d="M75,0 C75,14 50,14 50,28"
                    stroke="#E8EAED"
                    strokeWidth="1.5"
                    fill="none"
                    vectorEffect="non-scaling-stroke"
                  />
                </svg>

                {/* ── End Conversation ──────────────────────────────────── */}
                <div className="flex items-center gap-2.5 rounded-lg border border-red-border bg-red-bg px-4 py-2.5">
                  <span
                    className="material-symbols-rounded flex-shrink-0 text-[15px] text-red-text"
                    aria-hidden="true"
                  >
                    mark_chat_read
                  </span>
                  <div>
                    <p className="whitespace-nowrap text-xs font-semibold text-red-text">
                      End Conversation
                    </p>
                    <p className="whitespace-nowrap text-[10px] text-neutral-500">
                      Close the thread
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ── CTA ──────────────────────────────────────────────────────── */}
        <section
          className="border-b border-neutral-300"
          style={{ background: "var(--color-primary)" }}
        >
          <div className="mx-auto max-w-6xl px-6 py-16 sm:px-8 sm:py-20">
            <div className="flex flex-col gap-8 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2
                  className="m-0 text-2xl font-bold tracking-tight sm:text-3xl"
                  style={{ color: "var(--color-neutral-100)" }}
                >
                  Ready to unify your inbox?
                </h2>
                <p
                  className="mt-2 text-sm"
                  style={{ color: "var(--color-tertiary)" }}
                >
                  Set up in minutes. Free to get started.
                </p>
              </div>

              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-3 sm:flex-row sm:flex-shrink-0 sm:flex-wrap sm:items-center">
                  <Link
                    href="/signup"
                    className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-light sm:w-auto"
                  >
                    Create free account
                  </Link>

                  <TryItButton fullWidth />
                </div>

                <p
                  className="text-sm"
                  style={{ color: "var(--color-tertiary)" }}
                >
                  Already have an account?{" "}
                  <Link
                    href="/login"
                    className="font-semibold underline underline-offset-2 transition-opacity hover:opacity-80"
                    style={{ color: "var(--color-tertiary)" }}
                  >
                    Sign in
                  </Link>
                </p>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* ── Footer ──────────────────────────────────────────────────── */}
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

            <p className="m-0 text-xs text-neutral-500">
              &copy; {new Date().getFullYear()} RelayFlow &nbsp;&mdash;&nbsp;
              Built for developer-grade reliability.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
