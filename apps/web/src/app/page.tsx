import Link from "next/link";

import { TryItButton } from "@/components/landing/TryItButton";

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

const FLOW_NODES = [
  {
    icon: "chat",
    label: "Trigger",
    cls: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    icon: "data_object",
    label: "Set variable",
    cls: "border-purple-border bg-purple-bg text-purple-text",
  },
  {
    icon: "http",
    label: "HTTP request",
    cls: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    icon: "account_tree",
    label: "Condition",
    cls: "border-yellow-border bg-yellow-bg text-yellow-text",
  },
  {
    icon: "send",
    label: "Send reply",
    cls: "border-green-border bg-green-bg text-green-text",
  },
  {
    icon: "receipt_long",
    label: "Log event",
    cls: "border-purple-border bg-purple-bg text-purple-text",
  },
];

const PREVIEW_CONVOS = [
  {
    name: "Amara K.",
    preview: "Hi, I need help with my order #4821",
    time: "now",
    dot: "bg-secondary",
    badge: "bg-blue-bg text-blue-text",
    channel: "Telegram",
    active: true,
  },
  {
    name: "Dev Support",
    preview: "The webhook keeps timing out on POST…",
    time: "3m",
    dot: "bg-green-border",
    badge: "bg-green-bg text-green-text",
    channel: "WhatsApp",
    active: false,
  },
  {
    name: "Marcus L.",
    preview: "Following up on yesterday's issue",
    time: "1h",
    dot: "bg-yellow-border",
    badge: "bg-yellow-bg text-yellow-text",
    channel: "Email",
    active: false,
  },
  {
    name: "Yuki T.",
    preview: "Perfect, that worked! Thank you 🙏",
    time: "2h",
    dot: "bg-neutral-400",
    badge: "bg-blue-bg text-blue-text",
    channel: "Telegram",
    active: false,
  },
];

// ── Page ───────────────────────────────────────────────────────────────────

export default function Home() {
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
              <div className="inline-flex items-center gap-1.5 rounded-full border border-blue-border bg-blue-bg px-3 py-1 text-xs font-semibold text-blue-text">
                <span
                  className="material-symbols-rounded text-[13px]"
                  aria-hidden="true"
                >
                  bolt
                </span>
                Now in early access
              </div>

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
                  className="inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-secondary-dark"
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
                &nbsp;·&nbsp; Deploy in minutes
              </p>
            </div>

            {/* Right — inbox preview */}
            <div className="hidden lg:flex lg:justify-end">
              <div className="w-full max-w-sm overflow-hidden rounded-2xl border border-neutral-300 bg-neutral-100 shadow-xl">
                {/* Browser chrome */}
                <div className="flex items-center gap-1.5 border-b border-neutral-300 bg-neutral-200 px-3.5 py-2.5">
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
                  <span className="ml-auto flex h-5 min-w-[20px] items-center justify-center rounded-full bg-secondary px-1.5 text-[10px] font-bold text-white">
                    4
                  </span>
                </div>

                {/* Conversation list */}
                {PREVIEW_CONVOS.map((conv) => (
                  <div
                    key={conv.name}
                    className={`flex cursor-pointer items-start gap-3 border-b border-neutral-300 px-4 py-3 last:border-b-0 ${conv.active ? "bg-blue-bg/40" : "hover:bg-neutral-200/60"}`}
                  >
                    <div
                      className={`mt-1.5 h-2 w-2 flex-shrink-0 rounded-full ${conv.dot}`}
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-baseline justify-between gap-2">
                        <span className="truncate text-xs font-semibold text-primary">
                          {conv.name}
                        </span>
                        <span className="flex-shrink-0 text-[10px] text-neutral-500">
                          {conv.time}
                        </span>
                      </div>
                      <p className="mt-0.5 truncate text-xs text-neutral-600">
                        {conv.preview}
                      </p>
                      <span
                        className={`mt-1 inline-block rounded px-1.5 py-px text-[9px] font-semibold uppercase tracking-wide ${conv.badge}`}
                      >
                        {conv.channel}
                      </span>
                    </div>
                  </div>
                ))}

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
          <div className="mx-auto max-w-6xl px-6 py-16 sm:px-8 sm:py-20">
            <div className="mb-10">
              <h2 className="m-0 text-2xl font-bold tracking-tight text-primary sm:text-3xl">
                Automate any conversation pattern
              </h2>
              <p className="mt-2 text-sm text-neutral-600">
                Chain typed nodes into workflows. Trigger on any message, branch
                on conditions, call external APIs, send replies.
              </p>
            </div>

            {/* Flow */}
            <div
              role="img"
              aria-label="Example workflow: Trigger → Set variable → HTTP request → Condition → Send reply → Log event"
              className="flex w-full flex-wrap items-center gap-2 lg:flex-nowrap lg:justify-between"
            >
              {FLOW_NODES.map((node, i) => (
                <div
                  key={node.label}
                  className="flex flex-shrink-0 items-center gap-2"
                >
                  <div
                    className={`flex items-center gap-2 rounded-lg border px-3.5 py-2.5 text-sm font-medium ${node.cls}`}
                  >
                    <span
                      className="material-symbols-rounded text-[15px]"
                      aria-hidden="true"
                    >
                      {node.icon}
                    </span>
                    {node.label}
                  </div>

                  {i < FLOW_NODES.length - 1 && (
                    <span
                      className="material-symbols-rounded text-[18px] text-neutral-400"
                      aria-hidden="true"
                    >
                      arrow_forward
                    </span>
                  )}
                </div>
              ))}
            </div>

            <p className="mt-6 text-xs text-neutral-500">
              Every run is recorded. Inspect inputs, outputs, and timing for
              each step &mdash; no surprises.
            </p>
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
                  Set up in minutes. No credit card required.
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:flex-shrink-0 sm:flex-wrap sm:items-center">
                <Link
                  href="/signup"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-secondary-light sm:w-auto"
                >
                  Create free account
                </Link>

                <TryItButton fullWidth />

                <Link
                  href="/login"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-lg border px-5 py-2.5 text-sm font-semibold transition-colors sm:w-auto"
                  style={{
                    borderColor: "var(--color-tertiary-dark)",
                    color: "var(--color-tertiary)",
                  }}
                >
                  Sign in
                </Link>
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
