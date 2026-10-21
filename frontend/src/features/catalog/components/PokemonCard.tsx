import { Card } from "@heroui/react";
import { formatDexNumber, formatKg } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { AppLink } from "@/shared/ui/AppLink";
import { Sprite } from "@/shared/ui/Sprite";
import type { PokemonSummary } from "../types";
import { AbilityList } from "./AbilityList";

/**
 * Catalog card (US01, ui-ux.md §5.1): sprite, number, name, category, mass, abilities.
 * The whole card is one link, with hover and focus states.
 */
export function PokemonCard({ pokemon, priority = false }: { pokemon: PokemonSummary; priority?: boolean }) {
  const name = formatName(pokemon.name);
  return (
    <AppLink
      href={`/pokemon/${pokemon.name}`}
      className="group block h-full w-full rounded-3xl text-foreground no-underline hover:no-underline"
    >
      <Card
        className="h-full w-full gap-2 border border-transparent transition-colors group-hover:border-accent group-hover:bg-surface-secondary"
      >
        <div className="flex justify-center rounded-2xl bg-default">
          <Sprite src={pokemon.sprite} alt="" size={96} priority={priority} className="[image-rendering:pixelated]" />
        </div>
        <Card.Header className="gap-0">
          <span className="text-xs font-medium text-muted">{formatDexNumber(pokemon.id)}</span>
          <Card.Title className="text-base">{name}</Card.Title>
          <Card.Description className="text-sm">{pokemon.category}</Card.Description>
        </Card.Header>
        <Card.Content className="gap-1 text-sm">
          <p>
            <span className="sr-only">Mass: </span>
            {formatKg(pokemon.mass)}
          </p>
          <div>
            <span className="sr-only">Abilities: </span>
            <AbilityList abilities={pokemon.abilities} className="text-xs" />
          </div>
        </Card.Content>
      </Card>
    </AppLink>
  );
}
