import { Suspense } from "react";

import { Spinner } from "@/components/common/Spinner";
import { ProfileShell } from "@/components/profile/ProfileShell";

export default function ProfilePage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-full items-center justify-center">
          <Spinner size="lg" />
        </div>
      }
    >
      <ProfileShell />
    </Suspense>
  );
}
