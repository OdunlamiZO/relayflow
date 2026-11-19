// Docs: docs/codebase-components.md#voiceplayertesttsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoicePlayer } from "./VoicePlayer";

describe("VoicePlayer", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("plays and pauses the recording", async () => {
    const play = vi
      .spyOn(HTMLMediaElement.prototype, "play")
      .mockResolvedValue(undefined);
    const pause = vi
      .spyOn(HTMLMediaElement.prototype, "pause")
      .mockImplementation(() => {});

    render(<VoicePlayer src="/audio" durationSeconds={75} />);

    expect(screen.getByText("0:00 / 1:15")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Play voice message" })
    );
    expect(play).toHaveBeenCalled();

    await userEvent.click(
      screen.getByRole("button", { name: "Pause voice message" })
    );
    expect(pause).toHaveBeenCalled();
  });

  it("says when the recording can't be played", async () => {
    vi.spyOn(HTMLMediaElement.prototype, "play").mockRejectedValue(
      new Error("Not allowed")
    );

    render(<VoicePlayer src="/audio" durationSeconds={null} />);

    await userEvent.click(
      screen.getByRole("button", { name: "Play voice message" })
    );

    expect(
      await screen.findByText("Couldn't play this recording")
    ).toBeInTheDocument();
  });
});
