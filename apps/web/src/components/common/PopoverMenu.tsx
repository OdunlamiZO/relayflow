"use client";

import { type ReactNode, useEffect, useRef, useState } from "react";

export type PopoverMenuItem = {
  key: string;
  content: ReactNode;
  onSelect: () => void;
  selected?: boolean;
};

type Props = {
  trigger: (toggle: () => void) => ReactNode;
  items: PopoverMenuItem[];
};

export function PopoverMenu({ trigger, items }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    function handleMouseDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleMouseDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handleMouseDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  return (
    <div ref={containerRef} className="relative inline-flex">
      {trigger(() => setIsOpen((open) => !open))}

      {isOpen && (
        <ul
          role="menu"
          className="absolute left-0 top-full z-50 mt-1 min-w-40 rounded-xl border border-neutral-300 bg-neutral-100 p-1 shadow-lg"
        >
          {items.map((item) => (
            <li key={item.key}>
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setIsOpen(false);
                  item.onSelect();
                }}
                className="flex w-full items-center justify-between gap-3 rounded-lg px-2.5 py-1.5 text-left text-sm text-neutral-700 transition-colors hover:bg-neutral-200"
              >
                {item.content}
                {item.selected && (
                  <span
                    className="material-symbols-rounded text-[16px] leading-none text-secondary"
                    aria-hidden="true"
                  >
                    check
                  </span>
                )}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
