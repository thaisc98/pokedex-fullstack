import { baseApi } from "@/shared/api/baseApi";
import type {
  CollectedPokemon,
  CollectionPage,
  UpdateCollectedPokemonRequest,
} from "@/shared/api/types";

export type CollectionListArgs = { page: number; size: number; q?: string; tag?: string };
export type SyncResult = { item: CollectedPokemon; created: boolean };

/**
 * Collection endpoints (US03/US04). Tags: the list is `{Collection, LIST}`, each item
 * `{Collection, id}` (architecture.md §3.4).
 */
export const collectionApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    listCollection: build.query<CollectionPage, CollectionListArgs>({
      query: ({ page, size, q, tag }) => ({
        url: "/collection",
        // `page` is 1-based in the URL; the API is 0-based.
        params: { page: page - 1, size, ...(q ? { q } : {}), ...(tag ? { tag } : {}) },
      }),
      providesTags: (result) => [
        { type: "Collection", id: "LIST" },
        ...(result?.content.map((item) => ({ type: "Collection" as const, id: item.id })) ?? []),
      ],
    }),
    getCollected: build.query<CollectedPokemon, number>({
      query: (pokedexId) => `/collection/${pokedexId}`,
      providesTags: (_result, _error, id) => [{ type: "Collection", id }],
    }),
    sync: build.mutation<SyncResult, number>({
      query: (pokedexId) => ({ url: "/collection", method: "POST", body: { pokedexId } }),
      transformResponse: (item: CollectedPokemon, meta) => ({
        item,
        created: meta?.response?.status === 201,
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "Collection", id: "LIST" },
        { type: "Collection", id },
      ],
    }),
    updateCollected: build.mutation<
      CollectedPokemon,
      { pokedexId: number; body: UpdateCollectedPokemonRequest }
    >({
      query: ({ pokedexId, body }) => ({ url: `/collection/${pokedexId}`, method: "PUT", body }),
      // Write the server's answer into the cache only after it confirms (a 409 must not
      // leave an optimistic value behind). The list refetches.
      async onQueryStarted({ pokedexId }, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(collectionApi.util.upsertQueryData("getCollected", pokedexId, data));
        } catch {
          // The form shows the error.
        }
      },
      invalidatesTags: [{ type: "Collection", id: "LIST" }],
    }),
    deleteCollected: build.mutation<void, number>({
      query: (pokedexId) => ({ url: `/collection/${pokedexId}`, method: "DELETE" }),
      // Only the list: refetching the deleted item would just 404.
      invalidatesTags: [{ type: "Collection", id: "LIST" }],
    }),
  }),
});

export const {
  useListCollectionQuery,
  useGetCollectedQuery,
  useSyncMutation,
  useUpdateCollectedMutation,
  useDeleteCollectedMutation,
} = collectionApi;
