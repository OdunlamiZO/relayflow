import { describe, expect, it } from "vitest";

import {
  bulletLines,
  parseFormattedText,
  wrapSelection,
} from "./message-formatting";

describe("parseFormattedText", () => {
  it("parses each style", () => {
    expect(parseFormattedText("*bold* _italic_ ~gone~ `code`")).toEqual([
      { type: "bold", children: ["bold"] },
      " ",
      { type: "italic", children: ["italic"] },
      " ",
      { type: "strike", children: ["gone"] },
      " ",
      { type: "code", text: "code" },
    ]);
  });

  it("leaves code blocks unformatted", () => {
    expect(parseFormattedText("```\nlet *a* = 1;\n```")).toEqual([
      { type: "codeBlock", text: "\nlet *a* = 1;\n" },
    ]);
  });

  it("nests styles", () => {
    expect(parseFormattedText("*bold _and italic_*")).toEqual([
      {
        type: "bold",
        children: ["bold ", { type: "italic", children: ["and italic"] }],
      },
    ]);
  });

  it("ignores markers inside words and next to spaces", () => {
    expect(parseFormattedText("snake_case_name, 2*3*4, a * b * c")).toEqual([
      "snake_case_name, 2*3*4, a * b * c",
    ]);
  });

  it("never overlaps styles", () => {
    expect(parseFormattedText("*a _b* c_")).toEqual([
      { type: "bold", children: ["a _b"] },
      " c_",
    ]);
  });

  it("turns dash and star lines into bullets", () => {
    expect(parseFormattedText("- *Rice*\n* Beans")).toEqual([
      "• ",
      { type: "bold", children: ["Rice"] },
      "\n• Beans",
    ]);
  });

  it("leaves other dashes alone", () => {
    expect(parseFormattedText("a - b\n-5 degrees")).toEqual([
      "a - b\n-5 degrees",
    ]);
  });

  it("does not span lines", () => {
    expect(parseFormattedText("*one\ntwo*")).toEqual(["*one\ntwo*"]);
  });
});

describe("bulletLines", () => {
  it("bullets every selected line and skips ones that already are", () => {
    expect(bulletLines("Intro\nRice\n- Beans\nEnd", 7, 14)).toEqual({
      value: "Intro\n- Rice\n- Beans\nEnd",
      selectionStart: 6,
      selectionEnd: 20,
    });
  });

  it("bullets the current line when nothing is selected", () => {
    expect(bulletLines("Rice", 2, 2).value).toBe("- Rice");
  });
});

describe("wrapSelection", () => {
  it("wraps the selected text and keeps it selected", () => {
    expect(wrapSelection("say hello now", 4, 9, "*")).toEqual({
      value: "say *hello* now",
      selectionStart: 5,
      selectionEnd: 10,
    });
  });

  it("puts a code block on its own lines", () => {
    expect(wrapSelection("x", 0, 1, "```").value).toBe("```\nx\n```");
  });
});
