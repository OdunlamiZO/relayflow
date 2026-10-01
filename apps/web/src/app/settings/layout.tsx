import { AppHeader } from "@/components/workspace/AppHeader";
import { requireAuthentication } from "@/lib/server-authentication";

type Props = {
  children: React.ReactNode;
};

export default async function SettingsLayout({ children }: Props) {
  await requireAuthentication();

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-neutral-200">
      <AppHeader />

      <div className="flex-1 overflow-hidden">{children}</div>
    </div>
  );
}
