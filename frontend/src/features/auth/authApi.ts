import { baseApi } from "@/shared/api/baseApi";
import { clearSignedInHint, readSessionExpiry, setSignedInHint } from "@/shared/api/cookies";
import type { LoginRequest, LoginResponse, RegisterRequest, UserDto } from "@/shared/api/types";
import { sessionEnded, signedIn, userLoaded } from "./authSlice";

export const authApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    me: build.query<UserDto, void>({
      query: () => "/auth/me",
      providesTags: ["Me"],
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          // /auth/me has no expiry; the one saved at login re-arms the expiry warning.
          const expiresAt = readSessionExpiry();
          dispatch(expiresAt ? signedIn({ user: data, expiresAt }) : userLoaded(data));
        } catch {
          // 401 is handled in baseQuery (sessionEnded); anything else: treat as signed out.
          dispatch(sessionEnded());
        }
      },
    }),
    login: build.mutation<LoginResponse, LoginRequest>({
      query: (body) => ({ url: "/auth/login", method: "POST", body }),
      invalidatesTags: ["Me", "Collection"],
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          setSignedInHint(data.expiresAt);
          dispatch(signedIn(data));
        } catch {
          // The form shows the error.
        }
      },
    }),
    register: build.mutation<UserDto, RegisterRequest>({
      query: (body) => ({ url: "/auth/register", method: "POST", body }),
    }),
    logout: build.mutation<void, void>({
      query: () => ({ url: "/auth/logout", method: "POST" }),
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          await queryFulfilled;
        } finally {
          // Signed out locally whatever the server said; drop every cached private response.
          clearSignedInHint();
          dispatch(sessionEnded());
          dispatch(baseApi.util.resetApiState());
        }
      },
    }),
  }),
});

export const { useMeQuery, useLoginMutation, useRegisterMutation, useLogoutMutation } = authApi;
