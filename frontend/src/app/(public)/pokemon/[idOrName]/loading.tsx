import { Skeleton } from "@heroui/react";

/** Detail skeleton matching the final two-column layout (ui-ux.md §7). */
export default function PokemonDetailLoading() {
  return (
    <div className="flex flex-col gap-8" aria-busy="true">
      <span className="sr-only" role="status">
        Loading Pokémon…
      </span>
      <Skeleton className="h-4 w-40 rounded-lg" />
      <div className="grid gap-6 lg:grid-cols-2 lg:gap-10" aria-hidden="true">
        <Skeleton className="aspect-square w-full rounded-3xl" />
        <div className="flex flex-col gap-4">
          <Skeleton className="h-5 w-16 rounded-lg" />
          <Skeleton className="h-10 w-2/3 rounded-lg" />
          <div className="flex gap-2">
            <Skeleton className="h-7 w-20 rounded-full" />
            <Skeleton className="h-7 w-20 rounded-full" />
          </div>
          <Skeleton className="h-20 w-full rounded-2xl sm:max-w-sm" />
          <Skeleton className="h-10 w-48 rounded-xl" />
          <Skeleton className="h-24 w-full rounded-lg" />
        </div>
      </div>
      <div className="grid gap-8 lg:grid-cols-2 lg:gap-10" aria-hidden="true">
        <Skeleton className="h-72 rounded-3xl" />
        <Skeleton className="h-72 rounded-3xl" />
      </div>
    </div>
  );
}
