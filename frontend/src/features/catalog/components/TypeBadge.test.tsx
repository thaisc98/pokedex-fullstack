import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TypeBadge } from "./TypeBadge";

describe("TypeBadge", () => {
  it("should_show_the_type_as_a_text_label", () => {
    render(<TypeBadge type="electric" />);

    expect(screen.getByText("Electric")).toBeInTheDocument();
  });

  it("should_use_the_type_color_token_for_background_and_text", () => {
    const { container } = render(<TypeBadge type="fire" />);

    const chip = container.firstElementChild as HTMLElement;
    expect(chip.className).toContain("bg-type-fire");
    expect(chip.className).toContain("text-type-fire-foreground");
  });
});
