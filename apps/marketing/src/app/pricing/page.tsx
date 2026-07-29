import Link from "next/link";

import { Footer } from "@/components/Footer";
import { PricingForm } from "@/components/PricingForm";
import { configuration } from "@/lib/configuration";

// Paystack price/currency are runtime-only env vars (Dockerfile has no build ARG for them).
export const dynamic = "force-dynamic";

function formatPrice(): string {
  const amount = configuration.paystack.priceMinorUnits / 100;

  return new Intl.NumberFormat("en", {
    style: "currency",
    currency: configuration.paystack.currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

export default function PricingPage() {
  const price = formatPrice();
  const termDays = configuration.license.termDays;

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
            href="/docs/self-hosting"
            className="text-sm font-semibold text-neutral-600 transition-colors hover:text-accent"
          >
            Docs
          </Link>
        </div>
      </header>

      <main>
        <section className="border-b border-neutral-300 bg-neutral-100">
          <div className="mx-auto max-w-3xl px-6 py-16 sm:px-8 sm:py-20">
            <h1 className="text-[1.9rem] font-bold leading-[1.15] tracking-tight text-primary sm:text-[2.5rem]">
              Self-hosted RelayFlow,{" "}
              <span className="text-secondary">on your infrastructure.</span>
            </h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-neutral-600">
              Run RelayFlow on your own servers. Your data never leaves your
              infrastructure, and verification of your license happens entirely
              offline — no dependency on our servers being reachable.
            </p>

            <div className="mt-10 max-w-md rounded-2xl border border-neutral-300 bg-neutral-100 p-8 shadow-sm">
              <p className="text-sm font-semibold uppercase tracking-wide text-neutral-500">
                Self-hosted license
              </p>
              <p className="mt-2 text-4xl font-bold tracking-tight text-primary">
                {price}
              </p>
              <ul className="mt-6 flex flex-col gap-2 text-sm text-neutral-600">
                <li>
                  Valid for {termDays} days — renew any time by purchasing again
                </li>
                <li>One-time payment, no recurring billing</li>
                <li>Delivered instantly by email</li>
              </ul>

              <div className="mt-8">
                <PricingForm />
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
