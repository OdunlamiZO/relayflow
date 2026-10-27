"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

type ToastKind = "error" | "success";

type Toast = {
  id: number;
  kind: ToastKind;
  message: string;
};

type ToastInput = Omit<Toast, "id">;

type ToastContextValue = {
  showToast: (toast: ToastInput) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

type Props = {
  children: React.ReactNode;
};

const TOAST_STYLES: Record<ToastKind, string> = {
  error: "border-red-border bg-red-bg text-red-text",
  success: "border-green-border bg-green-bg text-green-text",
};

const TOAST_ICONS: Record<ToastKind, string> = {
  error: "error",
  success: "check_circle",
};

const TOAST_EVENT = "relayflow:toast";

export function ToastProvider({ children }: Props) {
  const showToast = useCallback((toast: ToastInput) => {
    window.dispatchEvent(
      new CustomEvent<ToastInput>(TOAST_EVENT, {
        detail: toast,
      })
    );
  }, []);

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport />
    </ToastContext.Provider>
  );
}

function ToastViewport() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  useEffect(() => {
    function handleToast(event: Event) {
      const toast = (event as CustomEvent<ToastInput>).detail;
      const id = Date.now();

      setToasts((current) => [...current, { ...toast, id }]);

      window.setTimeout(() => {
        dismissToast(id);
      }, 5000);
    }

    window.addEventListener(TOAST_EVENT, handleToast);

    return () => {
      window.removeEventListener(TOAST_EVENT, handleToast);
    };
  }, [dismissToast]);

  return (
    <div className="fixed right-4 top-4 z-[70] flex w-[min(22rem,calc(100vw-2rem))] flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="status"
          className={`flex items-center gap-2 rounded-lg border px-3 py-2.5 text-sm shadow-lg ${TOAST_STYLES[toast.kind]}`}
        >
          <span
            className="material-symbols-rounded shrink-0 text-[16px]"
            aria-hidden="true"
          >
            {TOAST_ICONS[toast.kind]}
          </span>

          <span className="flex-1 leading-5">{toast.message}</span>

          <button
            type="button"
            onClick={() => dismissToast(toast.id)}
            className="rounded p-0.5 opacity-70 transition-opacity hover:opacity-100"
            aria-label="Dismiss notification"
          >
            <span
              className="material-symbols-rounded text-[16px]"
              aria-hidden="true"
            >
              close
            </span>
          </button>
        </div>
      ))}
    </div>
  );
}

export function useToast() {
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error("useToast must be used within ToastProvider");
  }

  return context;
}
