// Docs: docs/codebase-components.md#auth-pages
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Create account — RelayFlow",
};

export default function SignupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
