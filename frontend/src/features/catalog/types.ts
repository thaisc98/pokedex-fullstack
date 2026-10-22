import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];

export type PokemonSummary = Schemas["PokemonSummaryDto"];
export type PokemonDetail = Schemas["PokemonDetailDto"];
export type PokemonType = Schemas["PokemonType"];
/** A catalog list item (`PokemonSummaryDto`, now with 1–2 `types`). */
export type CatalogItem = PokemonSummary;
export type PokemonPage = Omit<Schemas["PageResponse"], "content"> & { content: CatalogItem[] };
/** `GET /api/v1/pokemon/search` (generated from openapi.yaml). */
export type SearchResult = Schemas["PokemonNameRef"];
export type SearchResponse = Schemas["PokemonSearchResult"];
export type Ability = Schemas["Ability"];
export type BaseStat = Schemas["BaseStat"];
export type EvolutionTrigger = Schemas["EvolutionTrigger"];
export type EvolutionNode = Schemas["EvolutionNode"];
