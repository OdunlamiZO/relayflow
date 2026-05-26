const workflowNodes = [
  {
    label: "Message received",
    icon: "chat",
    className: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    label: "Set variable",
    icon: "data_object",
    className: "border-purple-border bg-purple-bg text-purple-text",
  },
  {
    label: "HTTP request",
    icon: "http",
    className: "border-blue-border bg-blue-bg text-blue-text",
  },
  {
    label: "Condition",
    icon: "account_tree",
    className: "border-yellow-border bg-yellow-bg text-yellow-text",
  },
  {
    label: "Send reply",
    icon: "send",
    className: "border-green-border bg-green-bg text-green-text",
  },
];

const conversations = ["Telegram customer", "Order support", "API follow-up"];

export default function Home() {
  return (
    <main className="min-h-screen bg-neutral-200 px-6 py-8 text-neutral-800 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <header className="mb-7">
          <p className="mb-2 flex items-center gap-2 font-bold text-accent">
            <span className="material-symbols-rounded" aria-hidden="true">
              account_tree
            </span>
            RelayFlow
          </p>
          <h1 className="m-0 text-3xl font-bold leading-tight text-primary sm:text-[34px]">
            Channel-agnostic messaging automation
          </h1>
          <p className="mt-4 max-w-3xl leading-7 text-neutral-600">
            Telegram is the pilot adapter. The platform model is built for
            omnichannel inbox, workflow variables, controllable HTTP steps, and
            transparent execution logs.
          </p>
        </header>

        <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.9fr)_minmax(360px,1.1fr)]">
          <section className="rounded-lg border border-neutral-300 bg-neutral-100 p-5">
            <h2 className="mb-4 mt-0 flex items-center gap-2 text-lg font-semibold text-primary">
              <span className="material-symbols-rounded" aria-hidden="true">
                inbox
              </span>
              Inbox Pilot
            </h2>
            <div className="grid gap-3">
              {conversations.map((item, index) => (
                <div
                  key={item}
                  className={`rounded-md border p-3 ${
                    index === 0
                      ? "border-blue-border bg-blue-bg text-blue-text"
                      : "border-neutral-300 bg-neutral-100 text-neutral-800"
                  }`}
                >
                  <strong>{item}</strong>
                  <p className="mb-0 mt-1.5 text-sm leading-6 text-neutral-600">
                    Last message routed through the normalized conversation
                    model.
                  </p>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-lg border border-neutral-300 bg-neutral-100 p-5">
            <h2 className="mb-4 mt-0 flex items-center gap-2 text-lg font-semibold text-primary">
              <span className="material-symbols-rounded" aria-hidden="true">
                receipt_long
              </span>
              Workflow Runtime
            </h2>
            <div className="flex flex-wrap gap-2.5">
              {workflowNodes.map((node) => (
                <div
                  key={node.label}
                  className={`flex min-w-[8.5rem] items-center gap-2 rounded-md border px-3 py-2.5 ${node.className}`}
                >
                  <span className="material-symbols-rounded" aria-hidden="true">
                    {node.icon}
                  </span>
                  {node.label}
                </div>
              ))}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
