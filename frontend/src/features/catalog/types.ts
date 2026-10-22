import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];

export type PokemonSummary = Schemas["PokemonSummaryDto"];
export type PokemonDetail = Schemas["PokemonDetailDto"];
export type PokemonType = Schemas["PokemonType"];
/**
 * A catalog list item. `types` comes with the backend search PR (list items gain 1–2 type
 * slugs); until that contract is on main the field may be missing, so it is optional here.
 * TODO: drop the intersection once `npm run gen:api` includes `types` on PokemonSummaryDto.
 */
export type CatalogItem = PokemonSummary & { types?: PokemonType[] };
export type PokemonPage = Omit<Schemas["PageResponse"], "content"> & { content: CatalogItem[] };
/** `GET /api/v1/pokemon/search` (backend PR in progress; types written from the agreed spec). */
export type SearchResult = { id: number; name: string };
export type SearchResponse = { results: SearchResult[] };
export type Ability = Schemas["Ability"];
export type BaseStat = Schemas["BaseStat"];
export type EvolutionTrigger = Schemas["EvolutionTrigger"];
export type EvolutionNode = Schemas["EvolutionNode"];
