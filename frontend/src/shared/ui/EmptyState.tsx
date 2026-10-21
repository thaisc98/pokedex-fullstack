import { Surface } from "@heroui/react";
import type { ReactNode } from "react";

type EmptyStateProps = {
  title: string;
  description?: string;
  /** Usually an `AppLink`/button, e.g. "Go to page 1". */
  action?: ReactNode;
  headingLevel?: 1 | 2;
};

/** Empty state (docs/ui-ux.md §6.4): illustration + message + one action, on a HeroUI Surface. */
export function EmptyState({ title, description, action, headingLevel = 2 }: EmptyStateProps) {
  const Heading = headingLevel === 1 ? "h1" : "h2";
  return (
    <Surface className="flex flex-col items-center gap-3 rounded-2xl px-6 py-12 text-center">
      <PokeballIcon />
      <Heading className="text-lg font-semibold">{title}</Heading>
      {description && <p className="max-w-md text-sm text-muted">{description}</p>}
      {action}
    </Surface>
  );
}

function PokeballIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 48 48" className="size-12 text-muted">
      <circle cx="24" cy="24" r="20" fill="none" stroke="currentColor" strokeWidth="3" />
      <path d="M4 24h14M30 24h14" stroke="currentColor" strokeWidth="3" />
      <circle cx="24" cy="24" r="6" fill="none" stroke="currentColor" strokeWidth="3" />
    </svg>
  );
}
