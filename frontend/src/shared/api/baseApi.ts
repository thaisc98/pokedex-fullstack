import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

/**
 * The single RTK Query API slice. Features add endpoints with
 * `baseApi.injectEndpoints(...)` (docs/architecture.md §3.4).
 *
 * P0: no endpoints yet. The full baseQuery (X-XSRF-TOKEN on non-GET requests,
 * ProblemDetail → ApiError, 401/403 handling) arrives with the first feature.
 */
export const baseApi = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({
    // Same origin as the page (docs/api/openapi.yaml `servers`); Next.js rewrites /api/*.
    baseUrl: "/api/v1",
    // The browser sends the httpOnly `access_token` cookie; JavaScript never sees it.
    credentials: "same-origin",
  }),
  endpoints: () => ({}),
});
