import { combineSlices, configureStore } from "@reduxjs/toolkit";
import { authSlice } from "@/features/auth/authSlice";
import { baseApi } from "@/shared/api/baseApi";

const rootReducer = combineSlices(authSlice, baseApi);

/**
 * Creates a new store. Called once per request/browser session inside
 * `StoreProvider`; never create a module-level store (it would be shared
 * between users on the server).
 */
export const makeStore = () =>
  configureStore({
    reducer: rootReducer,
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(baseApi.middleware),
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<typeof rootReducer>;
export type AppDispatch = AppStore["dispatch"];
