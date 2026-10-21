import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { EvolutionNode } from "../types";
import { EvolutionTree } from "./EvolutionTree";
import { triggerLabel } from "../lib/evolution";

const leaf = (speciesId: number, name: string, trigger: EvolutionNode["trigger"]): EvolutionNode => ({
  speciesId,
  name,
  trigger,
  evolvesTo: [],
});

const eevee: EvolutionNode = {
  speciesId: 133,
  name: "eevee",
  trigger: null,
  evolvesTo: [
    leaf(134, "vaporeon", { kind: "use-item", item: "water-stone" }),
    leaf(135, "jolteon", { kind: "use-item", item: "thunder-stone" }),
    leaf(136, "flareon", { kind: "use-item", item: "fire-stone" }),
    leaf(196, "espeon", { kind: "level-up" }),
    leaf(197, "umbreon", { kind: "level-up" }),
    leaf(470, "leafeon", { kind: "level-up" }),
    leaf(471, "glaceon", { kind: "level-up" }),
    leaf(700, "sylveon", { kind: "level-up" }),
  ],
};

const bulbasaur: EvolutionNode = {
  speciesId: 1,
  name: "bulbasaur",
  trigger: null,
  evolvesTo: [
    {
      speciesId: 2,
      name: "ivysaur",
      trigger: { kind: "level-up", minLevel: 16 },
      evolvesTo: [leaf(3, "venusaur", { kind: "level-up", minLevel: 32 })],
    },
  ],
};

describe("triggerLabel", () => {
  it.each([
    [{ kind: "level-up", minLevel: 16 } as const, "Level 16"],
    [{ kind: "level-up" } as const, "Level up"],
    [{ kind: "use-item", item: "thunder-stone" } as const, "Use Thunder Stone"],
    [{ kind: "trade" } as const, "Trade"],
    [{ kind: "other", name: "three-critical-hits" } as const, "Three Critical Hits"],
  ])("should_describe_%j_as_%s", (trigger, expected) => {
    expect(triggerLabel(trigger)).toBe(expected);
  });
});

describe("EvolutionTree", () => {
  it("should_render_all_eight_eevee_branches_as_links", () => {
    render(<EvolutionTree root={eevee} currentId={133} />);

    const links = screen.getAllByRole("link");
    expect(links).toHaveLength(9);
    for (const name of ["Vaporeon", "Jolteon", "Flareon", "Espeon", "Umbreon", "Leafeon", "Glaceon", "Sylveon"]) {
      expect(screen.getByRole("link", { name: new RegExp(name) })).toBeInTheDocument();
    }
    expect(screen.getByRole("link", { name: /Jolteon/ })).toHaveAttribute("href", "/pokemon/135");
  });

  it("should_mark_the_current_stage_with_aria_current", () => {
    render(<EvolutionTree root={eevee} currentId={133} />);

    expect(screen.getByRole("link", { name: /Eevee/ })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: /Vaporeon/ })).not.toHaveAttribute("aria-current");
  });

  it("should_show_each_trigger_in_words", () => {
    render(<EvolutionTree root={eevee} currentId={133} />);

    expect(screen.getByText("Use Thunder Stone")).toBeInTheDocument();
    expect(screen.getAllByText("Level up")).toHaveLength(5);
  });

  it("should_render_a_linear_chain_in_order_with_levels", () => {
    render(<EvolutionTree root={bulbasaur} currentId={2} />);

    const names = screen.getAllByRole("link").map((a) => within(a).getByText(/saur$/).textContent);
    expect(names).toEqual(["Bulbasaur", "Ivysaur", "Venusaur"]);
    expect(screen.getByText("Level 16")).toBeInTheDocument();
    expect(screen.getByText("Level 32")).toBeInTheDocument();
  });

  it("should_say_so_when_the_pokemon_does_not_evolve", () => {
    render(<EvolutionTree root={leaf(128, "tauros", null)} currentId={128} />);

    expect(screen.getByText(/does not evolve/i)).toBeInTheDocument();
  });
});
