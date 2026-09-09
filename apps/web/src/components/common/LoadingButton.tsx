import { Spinner } from "@/components/common/Spinner";

type Props = Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "disabled"> & {
  isLoading: boolean;
  disabled?: boolean;
};

// A <button> whose label renders invisibly (rather than being omitted) while
// loading, so the button keeps the width/height its label requires, with a
// spinner overlaid on top instead. `disabled` is combined with `isLoading`,
// so callers only need to pass whatever other condition should disable it.
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
