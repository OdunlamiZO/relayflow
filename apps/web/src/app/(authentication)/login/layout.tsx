// Docs: docs/codebase-components.md#auth-pages
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sign in — RelayFlow",
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
