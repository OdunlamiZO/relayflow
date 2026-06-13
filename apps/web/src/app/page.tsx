import { redirect } from "next/navigation";

import { getServerAuthenticationStatus } from "@/lib/server-authentication";

// The marketing site (relayflow.tech) is a separate app and owns the public
// landing page. This route only needs to send visitors to the right place
// inside the app.
export default async function Home() {
  const { authenticated } = await getServerAuthenticationStatus();

  redirect(authenticated ? "/inbox" : "/login");
}
