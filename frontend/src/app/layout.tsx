import type { Metadata } from "next";
import { headers } from "next/headers";
import { AppShell } from "@/shared/ui/AppShell";
import { Providers } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Pokédex",
    template: "%s · Pokédex",
  },
  description: "Browse Pokémon and curate the team's collection.",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Set per request by src/proxy.ts (CSP nonce). Reading headers makes every page dynamic,
  // which nonces require (Next.js CSP guide).
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  return (
    // next-themes sets the theme class on <html> before hydration.
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* React Aria reads the nonce from this tag for the <style> it injects (usePress). */}
        <meta property="csp-nonce" nonce={nonce} />
      </head>
      <body className="min-h-dvh bg-background text-foreground antialiased">
        <Providers nonce={nonce}>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  );
}
