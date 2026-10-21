import type { Metadata } from "next";
import { Suspense } from "react";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { CollectedDetail, DetailSkeleton } from "@/features/collection/components/CollectedDetail";

export const metadata: Metadata = { title: "Collected Pokémon" };

/** Read view + edit (US04): Client Component + RTK Query. */
export default function CollectedPokemonPage() {
  return (
    <Suspense fallback={<DetailSkeleton />}>
      <RequireAuth fallback={<DetailSkeleton />}>
        <CollectedDetail />
      </RequireAuth>
    </Suspense>
  );
}
