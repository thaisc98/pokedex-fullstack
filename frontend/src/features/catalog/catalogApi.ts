import { baseApi } from "@/shared/api/baseApi";
import type { SearchResponse } from "./types";

export const SEARCH_LIMIT = 10;

/** Public catalog search for the "Find a Pokémon" box (client-side, as you type). */
export const catalogApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchPokemon: build.query<SearchResponse, string>({
      query: (q) => ({ url: "/pokemon/search", params: { q, limit: SEARCH_LIMIT } }),
      keepUnusedDataFor: 300,
    }),
  }),
});

export const { useSearchPokemonQuery } = catalogApi;
