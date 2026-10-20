# ADR 0009 — HeroUI v3 as the UI component library

**Status:** Accepted · 2026-10-19

## Context
[ui-ux.md](../ui-ux.md) lists ~20 UI components: cards, pagination, tables, forms, dialogs, toasts, meters and so on. Building them ourselves to an accessible standard (keyboard support, focus handling, ARIA, screen-reader announcements) would take most of the frontend time. The grading criteria include "responsive, user-centric design", and the exercise asks for no console warnings.

Facts checked in the HeroUI docs on 2026-10-19 ([getting started](https://heroui.com/docs/react/getting-started), [quick start](https://heroui.com/docs/react/getting-started/quick-start), [theming](https://heroui.com/docs/react/getting-started/theming), [dark mode](https://heroui.com/docs/react/getting-started/dark-mode), [composition](https://heroui.com/docs/react/getting-started/composition), [toast](https://heroui.com/docs/react/components/toast)):
- **HeroUI v3** is stable, **built on React Aria Components**, and requires **React 19+** and **Tailwind CSS v4**.
- Packages: `@heroui/react` + `@heroui/styles`; CSS: `@import "tailwindcss"; @import "@heroui/styles";`
- **No Provider required.** Toasts need `<Toast.Provider />` mounted once.
- Theming via CSS variables (`--accent`, `--background`, `--surface`, `--danger`, `--radius`, …), mapped to Tailwind with `@theme inline`.
- Dark mode via the `dark` class / `data-theme` on `<html>`. For Next.js the docs show `next-themes` (`attribute="class"`, `suppressHydrationWarning`). Use **one** theme controller per app.
- Client-side router links through the `render` prop. The docs' own example wraps `next/link`.
- Apache-2.0 license.

## Decision
Use **HeroUI v3** for every component it provides. We build custom components only where it has none (app header, evolution tree, empty state) and assemble those from HeroUI parts.

- Install `@heroui/react` and `@heroui/styles`; React 19; Tailwind v4.
- Theme: one `src/styles/themes/pokedex.css` that overrides HeroUI's variables (brand red as `--accent`) and adds 18 Pokémon type colors through `@theme inline`.
- Dark mode: `next-themes` `ThemeProvider` (`attribute="class"`, `defaultTheme="system"`) as the single theme controller. Its anti-flash inline script gets the per-request CSP **nonce** from `proxy.ts` (ADR 0010).
- Routing: one `AppLink` wrapper in `src/shared/ui` that passes `next/link` through HeroUI's `render` prop (props and ref forwarded). Raw `<a href>` is never used for internal links.
- HeroUI's interactive components are used inside **Client Components**. Server Components pass data down as props. In P0, check whether `@heroui/react` ships `"use client"` directives.
- Toasts: the HeroUI `toast()` API with `<Toast.Provider />`. Toast state is **not** kept in Redux, to avoid holding the same state in two places.
- Forms: React Hook Form + Zod for state and validation; HeroUI `TextField` / `NumberField` / `ComboBox` / `TagGroup` for rendering, connected with `Controller`.
- AI assistants use the HeroUI MCP server (`@heroui/react-mcp`) so they work from v3 docs rather than v2 memory.

## Consequences
- ✅ React Aria accessibility (keyboard support, focus handling, ARIA patterns, toast announcements) for free, covering most of WCAG 2.2 AA.
- ✅ Uses the same Tailwind v4 we had already chosen; CSS-variable theming matches our token approach; HeroUI's docs cover Next.js directly.
- ✅ Dark mode and theming without a provider.
- ➖ React Aria events (`onPress`, `isDisabled`) differ from DOM ones (`onClick`, `disabled`). The team and AI tools must use the React Aria names.
- ➖ Component tests should use `@testing-library/user-event` (real pointer/keyboard sequences) rather than `fireEvent`.
- ⚠️ **AI tools often generate HeroUI v2 / NextUI code** (`@nextui-org/*`, `HeroUIProvider`, the `heroui()` Tailwind plugin in `tailwind.config.js`). All of it is wrong for v3. Listed in `AGENTS.md`.
- ⚠️ CSP compatibility (no injected inline `<style>`/`<script>`) is checked in P5 by the Playwright console guard on every page.

## Alternatives considered
- **shadcn/ui (Radix):** excellent, but its components are copied into the repo, so we would own and maintain them.
- **MUI:** mature, but its CSS-in-JS styling conflicts with Tailwind and needs a CSP nonce setup.
- **Hand-built on React Aria Components:** maximum control, but much slower to build.
