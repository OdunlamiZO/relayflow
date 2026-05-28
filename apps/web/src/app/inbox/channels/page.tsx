import { redirect } from "next/navigation";

import { ChannelsList } from "@/components/inbox/ChannelsList";
import { requireAuthentication } from "@/lib/server-authentication";

export default async function ChannelsPage({
  searchParams,
}: {
  searchParams: Promise<{ workspaceId?: string }>;
}) {
  await requireAuthentication();

  const { workspaceId } = await searchParams;

  if (!workspaceId) {
    redirect("/inbox");
  }

  return <ChannelsList workspaceId={workspaceId} />;
}
