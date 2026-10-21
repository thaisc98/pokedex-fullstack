import type { ReactNode } from "react";
import { AppButtonLink, AppLink } from "./AppLink";
import { MainNav, MobileMenu } from "./MainNav";
import { ThemeToggle } from "./ThemeToggle";

/**
 * App shell (docs/ui-ux.md §3.2): skip link, header with nav, <main id="content">, footer.
 * Landmarks: header / nav / main / footer.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col">
      {/* In-page fragment link, so a plain anchor (not a route link). */}
      <a
        href="#content"
        className="sr-only rounded-lg bg-accent px-4 py-2 font-semibold text-accent-foreground focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50"
      >
        Skip to content
      </a>

      <header className="sticky top-0 z-40 border-b border-separator bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center gap-4 px-4">
          <AppLink
            href="/pokemon"
            className="flex items-center gap-2 text-lg font-bold text-foreground no-underline hover:no-underline"
          >
            <LogoIcon />
            <span>Pokédex</span>
          </AppLink>

          <MainNav className="ml-4 hidden md:flex" />

          <div className="ml-auto flex items-center gap-2">
            <div className="hidden items-center gap-2 md:flex">
              <AppButtonLink href="/login" variant="ghost" size="sm">
                Log in
              </AppButtonLink>
              <AppButtonLink href="/register" variant="primary" size="sm">
                Register
              </AppButtonLink>
            </div>
            <ThemeToggle />
            <MobileMenu className="md:hidden" />
          </div>
        </div>
      </header>

      <main id="content" tabIndex={-1} className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 outline-none md:py-8">
        {children}
      </main>

      <footer className="border-t border-separator">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-6 text-sm text-muted">
          <p>Pokémon data from PokéAPI v2 (pokeapi.co)</p>
          <p>Pokémon and Pokémon names are trademarks of Nintendo.</p>
        </div>
      </footer>
    </div>
  );
}

function LogoIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 32 32" className="size-7">
      <circle cx="16" cy="16" r="14" className="fill-accent" />
      <path d="M2 16a14 14 0 0 0 28 0Z" className="fill-surface" />
      <path d="M2 16h28" className="stroke-foreground" strokeWidth="2" />
      <circle cx="16" cy="16" r="4.5" className="fill-surface stroke-foreground" strokeWidth="2" />
      <circle cx="16" cy="16" r="14" fill="none" className="stroke-foreground" strokeWidth="2" />
    </svg>
  );
}
