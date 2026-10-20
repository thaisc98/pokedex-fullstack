"use client";

import { useState, type ReactNode } from "react";
import { Provider } from "react-redux";
import { makeStore } from "./makeStore";

/**
 * Creates the Redux store once per mounted tree (i.e. per request on the
 * server and once in the browser). Never a module-level store.
 *
 * The RTK Next.js guide uses `useRef` + `if (!ref.current) ref.current = makeStore()`,
 * but React 19's `react-hooks/refs` lint rule rejects reading `ref.current`
 * during render. A lazy `useState` initializer gives the same guarantee
 * (runs once per mount, stable across re-renders) without touching refs.
 */
export function StoreProvider({ children }: { children: ReactNode }) {
  const [store] = useState(makeStore);
  return <Provider store={store}>{children}</Provider>;
}
