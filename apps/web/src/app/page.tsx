import { redirect } from "next/navigation";

import {
  getInstanceStatus,
  getServerAuthenticationStatus,
} from "@/lib/server-authentication";

// The marketing site (relayflow.tech) is a separate app and owns the public
// landing page. This route only needs to send visitors to the right place
// inside the app.
export default async function Home() {
  const { bootstrapped } = await getInstanceStatus();

  if (!bootstrapped) {
    redirect("/setup");
  }

  const { authenticated } = await getServerAuthenticationStatus();

  redirect(authenticated ? "/inbox" : "/login");
}
