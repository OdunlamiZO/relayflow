import { Suspense } from "react";

import { CheckoutCompleteContent } from "@/components/CheckoutCompleteContent";

export default function CheckoutCompletePage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-200 px-6 text-neutral-800">
      <Suspense fallback={null}>
        <CheckoutCompleteContent />
      </Suspense>
    </div>
  );
}
