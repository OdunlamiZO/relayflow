import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { ContactsShell } from "@/components/contacts/ContactsShell";

export const metadata = {
  title: "Contacts — RelayFlow",
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type WorkspaceItem = { id: string; name: string };

async function fetchWorkspaces(): Promise<WorkspaceItem[]> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  try {
    const res = await fetch(`${apiBaseUrl}/workspaces`, {
      headers: { Cookie: cookieHeader },
      cache: "no-store",
    });

    if (!res.ok) {
      return [];
    }

    return (await res.json()) as WorkspaceItem[];
  } catch {
    return [];
  }
}

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

  const workspaces = await fetchWorkspaces();

  if (workspaces.length > 0) {
    redirect(`/contacts?workspaceId=${workspaces[0].id}`);
  }

  redirect("/inbox");
}
