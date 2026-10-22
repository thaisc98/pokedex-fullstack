import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { CatalogItem } from "../types";
import { PokemonCard } from "./PokemonCard";

const bulbasaur: CatalogItem = {
  id: 1,
  name: "bulbasaur",
  sprite: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
  category: "Seed Pokémon",
  mass: 6.9,
  abilities: [
    { name: "overgrow", hidden: false },
    { name: "chlorophyll", hidden: true },
  ],
  types: ["grass", "poison"],
};

describe("PokemonCard", () => {
  it("should_show_the_type_chips_under_the_name", () => {
    render(<PokemonCard pokemon={bulbasaur} />);

    const types = within(screen.getByRole("list", { name: "Types" }));
    expect(types.getByText("Grass")).toBeInTheDocument();
    expect(types.getByText("Poison")).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveTextContent(/Bulbasaur.*Seed Pokémon.*Weight6\.9 kg.*Abilities/);
  });

  it("should_keep_the_sprite_decorative_so_the_name_is_not_read_twice", () => {
    const { container } = render(<PokemonCard pokemon={bulbasaur} />);

    expect(container.querySelector("img")).toHaveAttribute("alt", "");
  });
});
