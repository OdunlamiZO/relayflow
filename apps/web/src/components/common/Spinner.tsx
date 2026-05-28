type Size = "sm" | "md" | "lg";

type Props = {
  size?: Size;
  className?: string;
};

const sizeClasses: Record<Size, string> = {
  sm: "h-4 w-4 border-2",
  md: "h-6 w-6 border-2",
  lg: "h-8 w-8 border-[3px]",
};

export function Spinner({ size = "md", className = "" }: Props) {
  return (
    <div
      role="status"
      aria-label="Loading"
      className={`animate-spin rounded-full border-neutral-300 border-t-secondary ${sizeClasses[size]} ${className}`}
    />
  );
}
