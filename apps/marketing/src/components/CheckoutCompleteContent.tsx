"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

import { Spinner } from "@/components/Spinner";

const POLL_INTERVAL_MILLISECONDS = 2000;
const SLOW_POLL_THRESHOLD = 60; // ~2 minutes at a 2s interval

type OrderStatusResponse =
  | { status: "pending" | "awaiting_access_grant" | "failed" }
  | { status: "paid"; licenseKey: string; expiresAt: number };

export function CheckoutCompleteContent() {
  const searchParams = useSearchParams();
  const reference = searchParams.get("reference");

  const [statusResponse, setStatusResponse] =
    useState<OrderStatusResponse | null>(null);
  const [pollCount, setPollCount] = useState(0);
  const [notFound, setNotFound] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!reference || statusResponse?.status === "paid") {
      return;
    }

    let cancelled = false;

    async function poll() {
      const response = await fetch(`/api/orders/${reference}/status`);

      if (cancelled) {
        return;
      }

      if (response.status === 404) {
        setNotFound(true);

        return;
      }

      const body = (await response.json()) as OrderStatusResponse;

      setStatusResponse(body);
      setPollCount((count) => count + 1);
    }

    poll();
    const intervalId = setInterval(poll, POLL_INTERVAL_MILLISECONDS);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, [reference, statusResponse?.status]);

  function copyLicenseKey(licenseKey: string) {
    navigator.clipboard.writeText(licenseKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (!reference || notFound) {
    return (
      <div className="max-w-md text-center">
        <p className="text-sm text-neutral-600">
          We couldn&apos;t find that order.
        </p>
        <Link
          href="/pricing"
          className="mt-4 inline-block text-sm font-semibold text-secondary"
        >
          Back to pricing
        </Link>
      </div>
    );
  }

  if (statusResponse?.status === "failed") {
    return (
      <div className="max-w-md text-center">
        <p className="text-sm text-neutral-600">
          Something went wrong starting your checkout.
        </p>
        <Link
          href="/pricing"
          className="mt-4 inline-block text-sm font-semibold text-secondary"
        >
          Try again
        </Link>
      </div>
    );
  }

  if (statusResponse?.status === "paid") {
    return (
      <div className="w-full max-w-lg rounded-2xl border border-neutral-300 bg-neutral-100 p-8 shadow-sm">
        <h1 className="text-xl font-bold text-primary">
          Your license key is ready
        </h1>
        <p className="mt-2 text-sm text-neutral-600">
          We&apos;ve also emailed this to you. Set it as{" "}
          <code className="rounded bg-neutral-200 px-1 py-0.5 text-xs">
            RELAYFLOW_LICENSE_KEY
          </code>{" "}
          in your deployment&apos;s <code>.env</code> file.
        </p>

        <p className="mt-6 break-all rounded-lg bg-neutral-200 p-4 font-mono text-xs text-neutral-700">
          {statusResponse.licenseKey}
        </p>

        <button
          type="button"
          onClick={() => copyLicenseKey(statusResponse.licenseKey)}
          className="mt-4 rounded-lg border border-neutral-300 px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-200"
        >
          {copied ? "Copied" : "Copy license key"}
        </button>

        <p className="mt-6 text-sm text-neutral-600">
          Next,{" "}
          <Link
            href="/docs/self-hosting"
            className="font-semibold text-secondary"
          >
            follow the self-hosting guide
          </Link>{" "}
          to deploy — it has the exact docker-compose.yml and env files you
          need.
        </p>
      </div>
    );
  }

  const isAwaitingAccessGrant =
    statusResponse?.status === "awaiting_access_grant";
  const isSlow = pollCount >= SLOW_POLL_THRESHOLD;

  let message = "Waiting for payment confirmation…";

  if (isAwaitingAccessGrant && isSlow) {
    message =
      "Still setting up your registry access — we'll email your license key the moment it's ready. Feel free to close this tab.";
  } else if (isAwaitingAccessGrant) {
    message =
      "Payment received — setting up your access. This can take a little while.";
  } else if (isSlow) {
    message =
      "This is taking longer than expected — check your email, or try again if nothing arrives.";
  }

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <Spinner size="lg" />
      <p className="max-w-sm text-sm text-neutral-600">{message}</p>
    </div>
  );
}
