type Props = {
  icon: string;
  label: string;
  onClick: () => void;
  destructive?: boolean;
  disabled?: boolean;
};

export function IconButton({
  icon,
  label,
  onClick,
  destructive = false,
  disabled = false,
}: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={label}
      aria-label={label}
      className={`flex-shrink-0 rounded-lg p-1.5 text-neutral-400 transition-colors disabled:opacity-40 ${
        destructive ? "hover:text-red-text" : "hover:text-neutral-700"
      }`}
    >
      <span
        className="material-symbols-rounded text-[18px] leading-none"
        aria-hidden="true"
      >
        {icon}
      </span>
    </button>
  );
}
