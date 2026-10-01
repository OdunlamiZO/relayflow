import { AppHeader } from "@/components/workspace/AppHeader";
import { requireAuthentication } from "@/lib/server-authentication";

export const metadata = {
  title: "Profile — RelayFlow",
};

type Props = {
  children: React.ReactNode;
};

export default async function ProfileLayout({ children }: Props) {
  await requireAuthentication();

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-neutral-200">
      <AppHeader />

      <div className="flex-1 overflow-auto">{children}</div>
    </div>
  );
}
