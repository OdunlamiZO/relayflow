import { redirect } from "next/navigation";

import { getServerAuthenticationStatus } from "@/lib/server-authentication";

/**
 * Root route — never renders UI; acts as a server-side router.
 *
 * - Authenticated users (anonymous or real) → /inbox
 * - Everyone else → /login
 */
export default async function Home() {
  const { authenticated } = await getServerAuthenticationStatus();

  redirect(authenticated ? "/inbox" : "/login");
}
