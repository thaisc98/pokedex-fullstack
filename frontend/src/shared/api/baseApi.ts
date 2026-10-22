import { createApi } from "@reduxjs/toolkit/query/react";
import { baseQuery } from "./baseQuery";

/**
 * The single RTK Query API slice. Features add endpoints with
 * `baseApi.injectEndpoints(...)` (docs/architecture.md §3.4).
 */
export const baseApi = createApi({
  reducerPath: "api",
  baseQuery,
  tagTypes: ["Me", "Collection"],
  endpoints: () => ({}),
});
