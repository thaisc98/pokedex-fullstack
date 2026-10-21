import type { Metadata } from "next";
import { Suspense } from "react";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { CollectionList, ListSkeleton } from "@/features/collection/components/CollectionList";

export const metadata: Metadata = { title: "My collection" };

/** Authenticated, changing data → Client Component + RTK Query (architecture.md §3.2). */
export default function CollectionPage() {
  return (
    <Suspense fallback={<ListSkeleton />}>
      <RequireAuth fallback={<ListSkeleton />}>
        <CollectionList />
      </RequireAuth>
    </Suspense>
  );
}
