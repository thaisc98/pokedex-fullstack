"use client";

import { RouterProvider, Toast } from "@heroui/react";
import { useRouter } from "next/navigation";
import { ThemeProvider } from "next-themes";
import type { ReactNode } from "react";
import { SessionBootstrap } from "@/features/auth/SessionBootstrap";
import { StoreProvider } from "@/store/StoreProvider";

/**
 * Client providers for the whole app:
 * - next-themes: the single theme controller (architecture.md §3.3). Its inline
 *   pre-hydration script gets the CSP nonce.
 * - React Aria RouterProvider: HeroUI components with `href` (e.g. Breadcrumbs)
 *   navigate client-side through the App Router.
 * - Redux store, created per mount (makeStore in StoreProvider), plus the session restore.
 * - HeroUI Toast region, mounted once (ui-ux.md §6.1).
 */
export function Providers({ children, nonce }: { children: ReactNode; nonce?: string }) {
  const router = useRouter();
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
      nonce={nonce}
    >
      <RouterProvider navigate={(href) => router.push(href)}>
        <StoreProvider>
          <SessionBootstrap />
          {children}
          <Toast.Provider placement="bottom end" />
        </StoreProvider>
      </RouterProvider>
    </ThemeProvider>
  );
}
