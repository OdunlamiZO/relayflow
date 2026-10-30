#!/usr/bin/env node
// Keeps docs/codebase-components.md and the source in step:
// - every source file has one "// Docs: docs/codebase-components.md#<anchor>" line, and the
//   anchor is a heading in the doc;
// - every class, record, enum, and exported function, component, or constant is named in the doc;
// - every source file is linked from the section its "// Docs:" line points at, and every source
//   link in the doc points at a file whose "// Docs:" line points back at that section.

import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const repositoryRoot = join(fileURLToPath(import.meta.url), "..", "..");
const documentPath = "docs/codebase-components.md";
const linkPattern = /^\/\/ Docs: docs\/codebase-components\.md#([\w-]+)$/gm;
// A markdown link to ../apps/...; route groups like (authentication) put parentheses in the path.
const sourceLinkPattern = /\]\(\.\.\/(apps\/(?:[^()\s]|\([^()\s]*\))+)\)/g;

const sourceRoots = [
  "apps/api/src/main/java",
  "apps/api/src/test/java",
  "apps/web/src",
  "apps/marketing/src",
];

const skippedDirectories = new Set(["node_modules", ".next", "out"]);

function headingSlug(heading) {
  return heading
    .trim()
    .toLowerCase()
    .replace(/[^\w\- ]/g, "")
    .replace(/ /g, "-");
}

function* sourceFiles(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);

    if (entry.isDirectory()) {
      if (!skippedDirectories.has(entry.name)) {
        yield* sourceFiles(path);
      }

      continue;
    }

    if (/\.(java|tsx?)$/.test(entry.name) && !entry.name.endsWith(".d.ts")) {
      yield path;
    }
  }
}

function componentNames(path, source) {
  if (path.endsWith(".java")) {
    return [path.split("/").pop().replace(/\.java$/, "")];
  }

  // Route files are documented by their path, not their default export's name.
  if (path.includes("/app/") || /\.test\.tsx?$/.test(path)) {
    return [];
  }

  return [
    ...source.matchAll(
      /^export (?:default )?(?:async )?(?:function|const|class) (\w+)/gm
    ),
  ].map((match) => match[1]);
}

const documentText = readFileSync(join(repositoryRoot, documentPath), "utf-8");
const anchors = new Set(
  [...documentText.matchAll(/^#{1,6} (.+)$/gm)].map((match) =>
    headingSlug(match[1])
  )
);
const mentioned = new Set(
  [...documentText.matchAll(/`([A-Za-z_]\w*)/g)].map((match) => match[1])
);

// Source links in the doc, by the heading they sit under.
const linkedSections = new Map();
let currentSection = null;

for (const line of documentText.split("\n")) {
  const heading = line.match(/^#{1,6} (.+)$/);

  if (heading) {
    currentSection = headingSlug(heading[1]);

    continue;
  }

  for (const match of line.matchAll(sourceLinkPattern)) {
    linkedSections.set(match[1], [
      ...(linkedSections.get(match[1]) ?? []),
      currentSection,
    ]);
  }
}

const problems = [];
const anchorsByPath = new Map();

for (const root of sourceRoots) {
  for (const absolutePath of sourceFiles(join(repositoryRoot, root))) {
    const path = relative(repositoryRoot, absolutePath);
    const source = readFileSync(absolutePath, "utf-8");
    const links = [...source.matchAll(linkPattern)];

    if (links.length !== 1) {
      problems.push(
        `${path}: needs exactly one "// Docs: ${documentPath}#<anchor>" line (found ${links.length})`
      );
    } else if (!anchors.has(links[0][1])) {
      problems.push(
        `${path}: links to #${links[0][1]}, which isn't a heading in ${documentPath}`
      );
    } else {
      anchorsByPath.set(path, links[0][1]);
    }

    for (const name of componentNames(path, source)) {
      if (!mentioned.has(name)) {
        problems.push(`${path}: \`${name}\` isn't described in ${documentPath}`);
      }
    }
  }
}

for (const [path, anchor] of anchorsByPath) {
  if (!linkedSections.get(path)?.includes(anchor)) {
    problems.push(
      `${path}: isn't linked from #${anchor} in ${documentPath} (add "Source: [\`...\`](../${path})")`
    );
  }
}

for (const [path, sections] of linkedSections) {
  for (const section of sections) {
    if (!existsSync(join(repositoryRoot, path))) {
      problems.push(
        `${documentPath}#${section}: links to ${path}, which doesn't exist`
      );
    } else if (anchorsByPath.has(path) && anchorsByPath.get(path) !== section) {
      problems.push(
        `${documentPath}#${section}: links to ${path}, whose "// Docs:" line points at #${anchorsByPath.get(path)}`
      );
    }
  }
}

if (problems.length > 0) {
  console.error(
    `${documentPath} and the code are out of step:\n\n${problems.join("\n")}`
  );
  process.exit(1);
}

console.log(`${documentPath} matches the code.`);
