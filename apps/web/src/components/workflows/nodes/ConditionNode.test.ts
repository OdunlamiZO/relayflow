import { describe, expect, it } from "vitest";

import { type ConditionRule, operatorChange } from "./ConditionNode";

const condition: ConditionRule = {
  id: "cond-0",
  variable: "contact.tags.tier",
  operator: "eq",
  value: "gold",
};

describe("operatorChange", () => {
  it("keeps the value when the new operator compares against one", () => {
    const changed = { ...condition, ...operatorChange("contains") };

    expect(changed.operator).toBe("contains");
    expect(changed.value).toBe("gold");
  });

  it.each(["is_set", "is_not_set"] as const)(
    "clears the value when switching to %s, so it isn't saved",
    (operator) => {
      const changed = { ...condition, ...operatorChange(operator) };

      expect(changed.operator).toBe(operator);
      expect(JSON.parse(JSON.stringify(changed))).not.toHaveProperty("value");
    }
  );
});
