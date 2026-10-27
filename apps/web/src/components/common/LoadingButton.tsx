import { Spinner } from "@/components/common/Spinner";

type Props = Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "disabled"> & {
  isLoading: boolean;
  disabled?: boolean;
};

// The label stays rendered but invisible while loading so the button keeps its size.
export function LoadingButton({
  isLoading,
  disabled = false,
  className = "",
  children,
  ...rest
}: Props) {
  return (
    <button
      disabled={disabled || isLoading}
      className={`relative ${className}`.trim()}
      {...rest}
    >
      <span className={isLoading ? "invisible" : undefined}>{children}</span>

      {isLoading && (
        <Spinner
          size="sm"
          className="absolute inset-0 m-auto border-current/40 border-t-current"
        />
      )}
    </button>
  );
}
