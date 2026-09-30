import { describe, expect, it } from "vitest";

import { CHANGELOG_RELEASES, formatReleaseDate } from "./changelog";

const VERSION_PATTERN = /^\d+\.\d+\.\d+$/;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function compareVersions(left: string, right: string): number {
  const leftParts = left.split(".").map(Number);
  const rightParts = right.split(".").map(Number);

  for (let index = 0; index < 3; index++) {
    if (leftParts[index] !== rightParts[index]) {
      return leftParts[index] - rightParts[index];
    }
  }

  return 0;
}

describe("CHANGELOG_RELEASES", () => {
  it("uses semantic versions and ISO dates", () => {
    for (const release of CHANGELOG_RELEASES) {
      expect(release.version).toMatch(VERSION_PATTERN);
      expect(release.date).toMatch(DATE_PATTERN);
    }
  });

  it("lists releases newest first with no duplicate versions", () => {
    for (let index = 1; index < CHANGELOG_RELEASES.length; index++) {
      const newer = CHANGELOG_RELEASES[index - 1];
      const older = CHANGELOG_RELEASES[index];

      expect(compareVersions(newer.version, older.version)).toBeGreaterThan(0);
      expect(newer.date >= older.date).toBe(true);
    }
  });

  it("gives every release at least one change", () => {
    for (const release of CHANGELOG_RELEASES) {
      const entries = Object.values(release.changes).flat();

      expect(entries.length).toBeGreaterThan(0);
    }
  });
});

describe("formatReleaseDate", () => {
  it("formats the date without shifting it across time zones", () => {
    expect(formatReleaseDate("2026-09-01")).toBe("September 1, 2026");
  });
});
