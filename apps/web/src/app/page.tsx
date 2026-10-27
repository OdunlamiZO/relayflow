import { redirect } from "next/navigation";

import { serverAuthenticationApi } from "@/lib/server-authentication";

export default async function Home() {
  const { bootstrapped } = await serverAuthenticationApi.getInstanceStatus();

  if (!bootstrapped) {
    redirect("/setup");
  }

  const { authenticated } =
    await serverAuthenticationApi.getAuthenticationStatus();

  redirect(authenticated ? "/inbox" : "/login");
}
