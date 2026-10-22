import { Card } from "@heroui/react";
import { formatDexNumber, formatKg } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { AppLink } from "@/shared/ui/AppLink";
import { Sprite } from "@/shared/ui/Sprite";
import type { CatalogItem } from "../types";
import { AbilityList } from "./AbilityList";
import { TypeBadge } from "./TypeBadge";

/**
 * Catalog card (US01, ui-ux.md §5.1): the Pokémon is the hero. The 96 px sprite is drawn
 * at 160 px with crisp pixels; then number, name, types, category, weight and abilities.
 * The whole card is one link, with hover and focus states. The sprite is decorative
 * (`alt=""`): the visible name already names the card.
 */
export function PokemonCard({ pokemon, priority = false }: { pokemon: CatalogItem; priority?: boolean }) {
  const name = formatName(pokemon.name);
  return (
    <AppLink
      href={`/pokemon/${pokemon.name}`}
      className="group block h-full w-full rounded-3xl text-foreground no-underline hover:no-underline"
    >
      <Card className="h-full w-full gap-3 border border-transparent transition-colors group-hover:border-accent group-hover:bg-surface-secondary">
        <div className="flex h-36 items-center justify-center sm:h-40">
          <Sprite
            src={pokemon.sprite}
            alt=""
            size={96}
            // Ask the optimizer for a large width: it never enlarges, so the browser gets the
            // original 96 px pixels and scales them up crisply (image-rendering: pixelated).
            sizes="160px"
            priority={priority}
            className="size-36 transition-transform group-hover:scale-105 motion-reduce:transition-none sm:size-40 [image-rendering:pixelated]"
          />
        </div>
        <Card.Header className="gap-1">
          <span className="text-xs font-medium text-muted">{formatDexNumber(pokemon.id)}</span>
          <Card.Title className="text-base">{name}</Card.Title>
          {pokemon.types.length > 0 && (
            <ul className="flex flex-wrap gap-1" aria-label="Types">
              {pokemon.types.map((type) => (
                <li key={type}>
                  <TypeBadge type={type} size="sm" />
                </li>
              ))}
            </ul>
          )}
          <Card.Description className="text-sm">{pokemon.category}</Card.Description>
        </Card.Header>
        <Card.Content className="gap-2 text-sm">
          <p className="flex flex-col">
            <span className="text-xs text-muted">Weight</span>
            <span>{formatKg(pokemon.mass)}</span>
          </p>
          <div className="flex flex-col">
            <span className="text-xs text-muted">Abilities</span>
            <AbilityList abilities={pokemon.abilities} className="text-xs" />
          </div>
        </Card.Content>
      </Card>
    </AppLink>
  );
}
