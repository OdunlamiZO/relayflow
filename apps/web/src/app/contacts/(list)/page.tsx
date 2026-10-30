// Docs: docs/codebase-components.md#contacts-pages
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { ContactsShell } from "@/components/contacts/ContactsShell";
import { serverMessagingApi } from "@/lib/server-messaging-api";

export const metadata = {
  title: "Contacts — RelayFlow",
};

type SearchParams = Promise<{ workspaceId?: string }>;

type Props = {
  searchParams: SearchParams;
};

export default async function ContactsPage({ searchParams }: Props) {
  const { workspaceId } = await searchParams;

  if (workspaceId) {
    return (
      <Suspense
        fallback={
          <div className="flex h-full items-center justify-center">
            <Spinner size="lg" />
          </div>
        }
      >
        <ContactsShell workspaceId={workspaceId} />
      </Suspense>
    );
  }

  const workspaces = await serverMessagingApi.listWorkspaces();

  if (workspaces.length > 0) {
    redirect(`/contacts?workspaceId=${workspaces[0].id}`);
  }

  redirect("/inbox");
}
