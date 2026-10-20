import { configureStore } from "@reduxjs/toolkit";
import { baseApi } from "@/shared/api/baseApi";

/**
 * Creates a new store. Called once per request/browser session inside
 * `StoreProvider`; never create a module-level store (it would be shared
 * between users on the server).
 */
export const makeStore = () =>
  configureStore({
    reducer: {
      [baseApi.reducerPath]: baseApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(baseApi.middleware),
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
