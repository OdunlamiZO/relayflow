import type { SelectOption } from "@/components/common/Select";

const NON_COUNTRY_CODES = new Set(["EU", "EZ", "QO", "UN", "XA", "XB", "ZZ"]);

const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

export function countryOptions(locale?: string): SelectOption[] {
  const names = new Intl.DisplayNames(locale, {
    type: "region",
    fallback: "none",
  });
  const options: SelectOption[] = [];

  for (const first of LETTERS) {
    for (const second of LETTERS) {
      const code = first + second;
      const name = NON_COUNTRY_CODES.has(code) ? undefined : names.of(code);

      if (name) {
        options.push({ value: code, label: name });
      }
    }
  }

  return options.sort((a, b) => a.label.localeCompare(b.label));
}
