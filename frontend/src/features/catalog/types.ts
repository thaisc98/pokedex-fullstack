import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];

export type PokemonSummary = Schemas["PokemonSummaryDto"];
export type PokemonDetail = Schemas["PokemonDetailDto"];
export type PokemonPage = Omit<Schemas["PageResponse"], "content"> & { content: PokemonSummary[] };
export type PokemonType = Schemas["PokemonType"];
export type Ability = Schemas["Ability"];
export type BaseStat = Schemas["BaseStat"];
export type EvolutionTrigger = Schemas["EvolutionTrigger"];
export type EvolutionNode = Schemas["EvolutionNode"];
