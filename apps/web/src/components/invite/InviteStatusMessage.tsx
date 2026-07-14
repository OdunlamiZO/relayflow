import Link from "next/link";

type Status = "ACCEPTED" | "REVOKED" | "EXPIRED";

type Props = {
  status: Status;
};

export function InviteStatusMessage({ status }: Props) {
  if (status === "ACCEPTED") {
    return (
      <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
        <span
          className="material-symbols-rounded mb-3 text-[40px] text-green-text"
          aria-hidden="true"
        >
          check_circle
        </span>
        <h1 className="text-lg font-semibold text-primary">Already accepted</h1>
        <p className="mt-2 text-sm text-neutral-500">
          This invite has already been accepted.
        </p>
        <Link
          href="/inbox"
          className="mt-5 inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark"
        >
          Go to inbox
        </Link>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-neutral-300 bg-neutral-100 p-8 text-center shadow-sm">
      <span
        className="material-symbols-rounded mb-3 text-[40px] text-neutral-400"
        aria-hidden="true"
      >
        timer_off
      </span>
      <h1 className="text-lg font-semibold text-primary">
        {status === "EXPIRED" ? "Invite expired" : "Invite revoked"}
      </h1>
      <p className="mt-2 text-sm text-neutral-500">
        {status === "EXPIRED"
          ? "This invite link has expired. Ask the workspace owner to send a new one."
          : "This invite has been revoked by the workspace owner."}
      </p>
    </div>
  );
}
