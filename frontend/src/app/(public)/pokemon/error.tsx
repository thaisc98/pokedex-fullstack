"use client";

import { ErrorState } from "@/shared/ui/ErrorState";

/**
 * Unexpected errors in the catalog/detail segment. Expected API problems (503, …) are
 * rendered by the pages themselves with the correlationId; here we only have the digest
 * (Next.js hides server error details in production).
 */
export default function PokemonError({ error, retry }: { error: unknown; retry: () => void }) {
  const digest =
    error instanceof Error && "digest" in error && typeof error.digest === "string" ? error.digest : undefined;
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Something went wrong</h1>
      <ErrorState problem={{ type: "/problems/internal-error", status: 500 }} reference={digest} onRetry={retry} />
    </div>
  );
}
