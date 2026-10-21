import { Card, Skeleton } from "@heroui/react";

/** Skeleton grid (20 cards) matching the final layout, so nothing shifts (ui-ux.md §7, CLS budget). */
export default function CatalogLoading() {
  return (
    <div className="flex flex-col gap-6" aria-busy="true">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-3xl font-bold tracking-tight">Pokémon</h1>
        <span className="sr-only" role="status">
          Loading Pokémon…
        </span>
        <Skeleton className="h-4 w-40 rounded-lg" />
      </div>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-4 xl:grid-cols-5" aria-hidden="true">
        {Array.from({ length: 20 }, (_, i) => (
          <li key={i}>
            <Card className="gap-2">
              <Skeleton className="h-24 rounded-2xl" />
              <Skeleton className="h-3 w-10 rounded-lg" />
              <Skeleton className="h-4 w-3/4 rounded-lg" />
              <Skeleton className="h-3 w-1/2 rounded-lg" />
              <Skeleton className="h-3 w-1/3 rounded-lg" />
              <Skeleton className="h-3 w-2/3 rounded-lg" />
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
