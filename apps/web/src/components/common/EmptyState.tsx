type Props = {
  icon: string;
  title: string;
  description?: string;
  action?: React.ReactNode;
};

export function EmptyState({ icon, title, description, action }: Props) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      <span
        className="material-symbols-rounded text-neutral-400"
        style={{ fontSize: 40 }}
        aria-hidden="true"
      >
        {icon}
      </span>

      <div className="space-y-1">
        <p className="text-sm font-semibold text-neutral-700">{title}</p>

        {description && (
          <p className="text-xs text-neutral-500">{description}</p>
        )}
      </div>

      {action && <div className="mt-1">{action}</div>}
    </div>
  );
}
