const workflowNodes = [
  "Message received",
  "Set variable",
  "HTTP request",
  "Condition",
  "Send reply",
];

const conversations = ["Telegram customer", "Order support", "API follow-up"];

export default function Home() {
  return (
    <main className="min-h-screen bg-background px-6 py-8 text-foreground sm:px-8">
      <section className="mx-auto max-w-6xl">
        <header className="mb-7">
          <p className="mb-2 font-bold text-accent">RelayFlow</p>
          <h1 className="m-0 text-3xl font-bold leading-tight sm:text-[34px]">
            Channel-agnostic messaging automation
          </h1>
          <p className="mt-4 max-w-3xl leading-7 text-muted">
            Telegram is the pilot adapter. The platform model is built for
            omnichannel inbox, workflow variables, controllable HTTP steps, and
            transparent execution logs.
          </p>
        </header>

        <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.9fr)_minmax(360px,1.1fr)]">
          <section className="rounded-lg border border-line bg-panel p-5">
            <h2 className="mb-4 mt-0 text-lg font-semibold">Inbox Pilot</h2>
            <div className="grid gap-3">
              {conversations.map((item, index) => (
                <div
                  key={item}
                  className={`rounded-md border p-3 ${
                    index === 0
                      ? "border-blue-100 bg-blue-50"
                      : "border-slate-200 bg-white"
                  }`}
                >
                  <strong>{item}</strong>
                  <p className="mb-0 mt-1.5 text-sm leading-6 text-muted">
                    Last message routed through the normalized conversation
                    model.
                  </p>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-lg border border-line bg-panel p-5">
            <h2 className="mb-4 mt-0 text-lg font-semibold">
              Workflow Runtime
            </h2>
            <div className="flex flex-wrap gap-2.5">
              {workflowNodes.map((node) => (
                <div
                  key={node}
                  className="min-w-[8.5rem] rounded-md border border-line bg-slate-50 px-3 py-2.5"
                >
                  {node}
                </div>
              ))}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
