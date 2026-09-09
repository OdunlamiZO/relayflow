import Link from "next/link";

type Variant = "nav" | "hero" | "cta";

const VARIANT_CLASSNAMES: Record<Variant, string> = {
  nav: "rounded-lg bg-secondary px-3 py-2 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark",
  hero: "inline-flex items-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-dark",
  cta: "inline-flex w-full items-center justify-center gap-2 rounded-lg bg-secondary px-5 py-2.5 text-sm font-semibold text-neutral-100 transition-colors hover:bg-secondary-light sm:w-auto",
};

type Props = {
  variant: Variant;
  href: string;
  children: React.ReactNode;
};

export function CtaButton({ variant, href, children }: Props) {
  return (
    <Link href={href} className={VARIANT_CLASSNAMES[variant]}>
      {children}
    </Link>
  );
}
