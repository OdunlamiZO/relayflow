"use client";

import { useState } from "react";

import { ConfirmModal } from "@/components/common/ConfirmModal";
import { Spinner } from "@/components/common/Spinner";
import { useToast } from "@/components/providers/ToastProvider";
import { useCancelSubscription } from "@/hooks/use-cancel-subscription";
import { usePlans } from "@/hooks/use-plans";
import { useStartCheckout } from "@/hooks/use-start-checkout";
import { useSubscription } from "@/hooks/use-subscription";
import { errorMessage } from "@/lib/error-message";
import {
  type Plan,
  type PlanInfo,
  type Subscription,
} from "@/lib/messaging-api";

type Props = {
  workspaceId: string;
  isOwner: boolean;
};

// ── Tier grouping ──────────────────────────────────────────────────────────

type TierGroup = {
  key: string;
  label: string;
  monthly: PlanInfo | undefined;
  annual: PlanInfo | undefined;
};

function tierKey(plan: string): string {
  return plan.replace(/_(MONTHLY|ANNUAL)$/, "");
}

function tierLabel(plan: string): string {
  return tierKey(plan)
    .split("_")
    .map((w) => w[0] + w.slice(1).toLowerCase())
    .join(" ");
}

function planLabel(plan: string): string {
  if (plan === "FREE") return "Free";
  const label = tierLabel(plan);
  if (plan.endsWith("_MONTHLY")) return `${label} (monthly)`;
  if (plan.endsWith("_ANNUAL")) return `${label} (annual)`;

  return label;
}

function groupPlansByTier(plans: PlanInfo[]): TierGroup[] {
  const map = new Map<string, TierGroup>();

  for (const plan of plans) {
    const key = tierKey(plan.plan);
    if (key === "FREE") continue;

    if (!map.has(key)) {
      map.set(key, {
        key,
        label: tierLabel(plan.plan),
        monthly: undefined,
        annual: undefined,
      });
    }

    const group = map.get(key)!;
    if (plan.billingInterval === "monthly") group.monthly = plan;
    else if (plan.billingInterval === "annual") group.annual = plan;
  }

  return Array.from(map.values());
}

// ── Helpers ────────────────────────────────────────────────────────────────

function formatNgn(amount: number): string {
  return new Intl.NumberFormat("en-NG", {
    style: "currency",
    currency: "NGN",
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-NG", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function limitLabel(value: number | null): string {
  if (value === null || value >= 2_147_483_647) return "Unlimited";

  return String(value);
}

// ── Sub-components ─────────────────────────────────────────────────────────

function StatusBanner({ subscription }: { subscription: Subscription }) {
  if (subscription.status === "PAST_DUE") {
    return (
      <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
        <span
          className="material-symbols-rounded mt-0.5 text-[18px] leading-none text-red-500"
          aria-hidden="true"
        >
          warning
        </span>
        <span>
          Your subscription payment is overdue. Please update your payment
          details with your payment provider to avoid losing access.
        </span>
      </div>
    );
  }

  if (subscription.status === "CANCELLATION_SCHEDULED") {
    return (
      <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        <span
          className="material-symbols-rounded mt-0.5 text-[18px] leading-none text-amber-500"
          aria-hidden="true"
        >
          info
        </span>
        <span>
          Your subscription is cancelled and will end on{" "}
          <strong>
            {subscription.currentPeriodEnd
              ? formatDate(subscription.currentPeriodEnd)
              : "the end of this billing period"}
          </strong>
          . You keep full access until then.
        </span>
      </div>
    );
  }

  return null;
}

function DowngradeBanner({ subscription }: { subscription: Subscription }) {
  const channels = subscription.downgradeLockedChannels ?? 0;
  const workflows = subscription.downgradeLockedWorkflows ?? 0;

  if (channels === 0 && workflows === 0) return null;

  const parts: string[] = [];
  if (channels > 0)
    parts.push(
      `${channels} channel ${channels === 1 ? "account" : "accounts"}`
    );
  if (workflows > 0)
    parts.push(
      `${workflows} workflow ${workflows === 1 ? "definition" : "definitions"}`
    );

  return (
    <div className="flex items-start gap-3 rounded-xl border border-neutral-300 bg-neutral-100 px-4 py-3 text-sm text-neutral-700">
      <span
        className="material-symbols-rounded mt-0.5 text-[18px] leading-none text-neutral-400"
        aria-hidden="true"
      >
        lock
      </span>
      <span>
        {parts.join(" and ")} were disabled when your workspace was downgraded
        to the Free plan. Upgrade to re-enable them.
      </span>
    </div>
  );
}

function CurrentPlanCard({
  subscription,
  isOwner,
  onCancel,
  cancelling,
}: {
  subscription: Subscription;
  isOwner: boolean;
  onCancel: () => void;
  cancelling: boolean;
}) {
  const isPaid = subscription.plan !== "FREE";
  const canCancel =
    isOwner &&
    isPaid &&
    subscription.status !== "CANCELLATION_SCHEDULED" &&
    subscription.status !== "CANCELLED";

  return (
    <div className="rounded-xl border border-neutral-200 bg-neutral-100 p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Current plan
          </p>

          <p className="mt-1 text-lg font-semibold text-neutral-900">
            {planLabel(subscription.plan)}
          </p>

          {isPaid && subscription.priceNgn !== null && (
            <p className="mt-0.5 text-sm text-neutral-500">
              {formatNgn(subscription.priceNgn)}
              {subscription.billingInterval === "monthly"
                ? " / month"
                : " / year"}
            </p>
          )}

          {isPaid && subscription.currentPeriodEnd && (
            <p className="mt-0.5 text-sm text-neutral-500">
              {subscription.status === "CANCELLATION_SCHEDULED"
                ? "Access until "
                : "Renews "}
              {formatDate(subscription.currentPeriodEnd)}
            </p>
          )}
        </div>

        <span
          className={`mt-1 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
            subscription.status === "ACTIVE"
              ? "bg-green-100 text-green-700"
              : subscription.status === "PAST_DUE"
                ? "bg-red-100 text-red-700"
                : subscription.status === "CANCELLATION_SCHEDULED"
                  ? "bg-amber-100 text-amber-700"
                  : "bg-neutral-200 text-neutral-600"
          }`}
        >
          {subscription.status === "ACTIVE"
            ? "Active"
            : subscription.status === "PAST_DUE"
              ? "Past due"
              : subscription.status === "CANCELLATION_SCHEDULED"
                ? "Cancelling"
                : "Cancelled"}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-3 gap-3 border-t border-neutral-200 pt-4">
        <div>
          <p className="text-xs text-neutral-400">Channels</p>
          <p className="mt-0.5 text-sm font-medium text-neutral-800">
            {limitLabel(subscription.maxChannelAccounts)}
          </p>
        </div>

        <div>
          <p className="text-xs text-neutral-400">Workflows</p>
          <p className="mt-0.5 text-sm font-medium text-neutral-800">
            {limitLabel(subscription.maxWorkflows)}
          </p>
        </div>

        <div>
          <p className="text-xs text-neutral-400">Members</p>
          <p className="mt-0.5 text-sm font-medium text-neutral-800">
            {limitLabel(subscription.maxMembersPerWorkspace)}
          </p>
        </div>
      </div>

      {canCancel && (
        <div className="mt-4 border-t border-neutral-200 pt-4">
          <button
            onClick={onCancel}
            disabled={cancelling}
            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-neutral-500 transition-colors hover:bg-red-bg hover:text-red-text disabled:cursor-not-allowed disabled:opacity-50"
          >
            {cancelling && <Spinner size="sm" />}
            Cancel subscription
          </button>
        </div>
      )}
    </div>
  );
}

function TierCard({
  tier,
  onUpgrade,
  upgrading,
  upgradingPlan,
  disabled,
}: {
  tier: TierGroup;
  onUpgrade: (plan: Plan) => void;
  upgrading: boolean;
  upgradingPlan: Plan | undefined;
  disabled: boolean;
}) {
  const [cycle, setCycle] = useState<"monthly" | "annual">(
    tier.monthly ? "monthly" : "annual"
  );

  const selected = cycle === "monthly" ? tier.monthly : tier.annual;
  const hasBothCycles = tier.monthly !== undefined && tier.annual !== undefined;

  const savingsPct =
    hasBothCycles && tier.monthly!.priceNgn && tier.annual!.priceNgn
      ? Math.round(
          ((tier.monthly!.priceNgn * 12 - tier.annual!.priceNgn) /
            (tier.monthly!.priceNgn * 12)) *
            100
        )
      : null;

  return (
    <div className="rounded-xl border border-neutral-200 bg-neutral-100 p-5">
      {/* Billing cycle toggle + savings badge */}
      {hasBothCycles && (
        <div className="flex items-center gap-2.5">
          <div className="flex rounded-lg border border-neutral-200 bg-white p-0.5">
            <button
              onClick={() => setCycle("monthly")}
              className={`rounded-md px-3 py-1 text-sm font-medium transition-colors ${
                cycle === "monthly"
                  ? "bg-neutral-900 text-white"
                  : "text-neutral-500 hover:text-neutral-900"
              }`}
            >
              Monthly
            </button>

            <button
              onClick={() => setCycle("annual")}
              className={`rounded-md px-3 py-1 text-sm font-medium transition-colors ${
                cycle === "annual"
                  ? "bg-neutral-900 text-white"
                  : "text-neutral-500 hover:text-neutral-900"
              }`}
            >
              Annual
            </button>
          </div>

          {savingsPct !== null && (
            <span
              className={`rounded-full border px-2 py-0.5 text-xs font-semibold transition-colors ${
                cycle === "annual"
                  ? "border-green-200 bg-green-50 text-green-700"
                  : "border-neutral-200 bg-white text-neutral-400"
              }`}
            >
              Save {savingsPct}%
            </span>
          )}
        </div>
      )}

      {/* Plan name + price */}
      <p
        className={`text-sm font-semibold text-neutral-900 ${hasBothCycles ? "mt-4" : ""}`}
      >
        {tier.label}
      </p>

      {selected?.priceNgn != null ? (
        <>
          <p className="mt-1 text-2xl font-bold text-neutral-900">
            {formatNgn(selected.priceNgn)}
            <span className="ml-1 text-sm font-normal text-neutral-500">
              {cycle === "annual" ? "/ yr" : "/ mo"}
            </span>
          </p>

          {cycle === "annual" && tier.monthly?.priceNgn != null && (
            <p className="mt-0.5 text-xs text-neutral-400">
              vs {formatNgn(tier.monthly.priceNgn * 12)} billed monthly
            </p>
          )}
        </>
      ) : null}

      {/* Feature list */}
      <ul className="mt-4 space-y-2 text-sm text-neutral-600">
        <li className="flex items-center gap-2">
          <span
            className="material-symbols-rounded text-[16px] leading-none text-neutral-400"
            aria-hidden="true"
          >
            hub
          </span>
          {limitLabel(selected?.maxChannelAccounts ?? null)} channel accounts
        </li>

        <li className="flex items-center gap-2">
          <span
            className="material-symbols-rounded text-[16px] leading-none text-neutral-400"
            aria-hidden="true"
          >
            account_tree
          </span>
          {limitLabel(selected?.maxWorkflows ?? null)} workflows
        </li>

        <li className="flex items-center gap-2">
          <span
            className="material-symbols-rounded text-[16px] leading-none text-neutral-400"
            aria-hidden="true"
          >
            group
          </span>
          {limitLabel(selected?.maxMembersPerWorkspace ?? null)} members
        </li>
      </ul>

      {/* CTA */}
      {selected?.upgradeAvailable && (
        <button
          onClick={() => selected && onUpgrade(selected.plan)}
          disabled={upgrading || disabled}
          className="mt-4 flex w-full items-center justify-center gap-1.5 rounded-lg bg-secondary px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {upgrading && upgradingPlan === selected.plan && (
            <Spinner size="sm" />
          )}
          Upgrade to {tier.label}
        </button>
      )}
    </div>
  );
}

// ── Main component ─────────────────────────────────────────────────────────

export function BillingPanel({ workspaceId, isOwner }: Props) {
  const { data: subscription, isLoading: subLoading } =
    useSubscription(workspaceId);
  const { data: plans, isLoading: plansLoading } = usePlans();
  const startCheckout = useStartCheckout(workspaceId);
  const cancelSubscription = useCancelSubscription(workspaceId);
  const { showToast } = useToast();

  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);

  function handleUpgrade(plan: Plan) {
    startCheckout.mutate(plan, {
      onError: (err) =>
        showToast({ kind: "error", message: errorMessage(err) }),
    });
  }

  function handleCancelConfirm() {
    cancelSubscription.mutate(undefined, {
      onSuccess: () => setConfirmCancelOpen(false),
      onError: (err) => {
        setConfirmCancelOpen(false);
        showToast({ kind: "error", message: errorMessage(err) });
      },
    });
  }

  if (subLoading || plansLoading) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
        <div className="flex items-center gap-2 text-sm text-neutral-500">
          <Spinner size="sm" />
          Loading billing info…
        </div>
      </div>
    );
  }

  if (!subscription) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
        <p className="text-sm text-neutral-500">
          Unable to load subscription. Please refresh and try again.
        </p>
      </div>
    );
  }

  const allTiers = groupPlansByTier(plans ?? []);
  const currentTier = tierKey(subscription.plan);
  const upgradeableTiers = allTiers.filter(
    (t) =>
      t.key !== currentTier &&
      (t.monthly?.upgradeAvailable || t.annual?.upgradeAvailable)
  );

  return (
    <div className="mx-auto max-w-2xl px-6 py-8 sm:px-8">
      {/* Page title */}
      <h2 className="text-base font-semibold text-neutral-900">
        Billing &amp; subscription
      </h2>

      <p className="mt-1 text-sm text-neutral-500">
        Manage your plan and billing details.
      </p>

      {/* Status banners */}
      <div className="mt-6 flex flex-col gap-3">
        <StatusBanner subscription={subscription} />
        <DowngradeBanner subscription={subscription} />
      </div>

      {/* Current plan */}
      <section className="mt-6">
        <CurrentPlanCard
          subscription={subscription}
          isOwner={isOwner}
          onCancel={() => setConfirmCancelOpen(true)}
          cancelling={cancelSubscription.isPending}
        />
      </section>

      {/* Upgrade section — one card per upgradeable tier */}
      {upgradeableTiers.length > 0 && (
        <section className="mt-8">
          <p className="text-xs font-semibold uppercase tracking-wide text-neutral-400">
            Upgrade your plan
          </p>

          {subscription.upgradeRecommended && (
            <p className="mt-1.5 text-sm text-neutral-600">
              You&apos;re on the Free plan. Upgrade to unlock unlimited
              channels, workflows, and team members.
            </p>
          )}

          <div className="mt-4 flex flex-col gap-4">
            {upgradeableTiers.map((tier) => (
              <TierCard
                key={tier.key}
                tier={tier}
                onUpgrade={handleUpgrade}
                upgrading={startCheckout.isPending}
                upgradingPlan={startCheckout.variables}
                disabled={startCheckout.isPending}
              />
            ))}
          </div>
        </section>
      )}

      {/* Cancel confirmation modal */}
      {confirmCancelOpen && (
        <ConfirmModal
          title="Cancel subscription"
          description="Your workspace will keep Pro access until the end of the current billing period. After that it reverts to the Free plan."
          confirmLabel="Cancel subscription"
          destructive
          isPending={cancelSubscription.isPending}
          onConfirm={handleCancelConfirm}
          onCancel={() => setConfirmCancelOpen(false)}
        />
      )}
    </div>
  );
}
