import type { Metadata } from "next";
import { StoreProvider } from "@/store/StoreProvider";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Pokédex",
    template: "%s · Pokédex",
  },
  description: "Browse Pokémon and curate the team's collection.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body className="bg-background text-foreground antialiased">
        <StoreProvider>{children}</StoreProvider>
      </body>
    </html>
  );
}
