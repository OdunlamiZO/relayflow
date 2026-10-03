type Props = {
  left: string;
  className: string;
  title?: string;
  children: React.ReactNode;
};

export function OutputLabel({ left, className, title, children }: Props) {
  return (
    <span
      className={`absolute -translate-x-1/2 whitespace-nowrap text-[10px] leading-none ${className}`}
      style={{ left, bottom: 8 }}
      title={title}
    >
      {children}
    </span>
  );
}
