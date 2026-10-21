import { Breadcrumbs } from "@heroui/react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { AddToCollectionButton } from "@/features/catalog/components/AddToCollectionButton";
import { EvolutionTree } from "@/features/catalog/components/EvolutionTree";
import { StatMeters } from "@/features/catalog/components/StatMeters";
import { TypeBadge } from "@/features/catalog/components/TypeBadge";
import type { PokemonDetail } from "@/features/catalog/types";
import { CatalogError, getPokemonDetail } from "@/server/catalogClient";
import { formatDexNumber, formatKg, formatMeters } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import type { ProblemDetail } from "@/shared/lib/problemCopy";
import { ErrorState } from "@/shared/ui/ErrorState";
import { Sprite } from "@/shared/ui/Sprite";

type Result = { ok: true; pokemon: PokemonDetail } | { ok: false; problem: ProblemDetail };

/** One fetch per request, shared by generateMetadata and the page. 400/404 → not-found. */
const load = cache(async (idOrName: string): Promise<Result> => {
  try {
    return { ok: true, pokemon: await getPokemonDetail(idOrName) };
  } catch (error) {
    if (!(error instanceof CatalogError)) throw error;
    if (error.status === 404 || error.status === 400) notFound();
    return { ok: false, problem: error.problem };
  }
});

export async function generateMetadata(props: PageProps<"/pokemon/[idOrName]">): Promise<Metadata> {
  const { idOrName } = await props.params;
  const result = await load(idOrName);
  return { title: result.ok ? formatName(result.pokemon.name) : "Pokémon unavailable" };
}

/** US02 detail: Server Component (Next.js 16: params is a Promise). */
export default async function PokemonDetailPage(props: PageProps<"/pokemon/[idOrName]">) {
  const { idOrName } = await props.params;
  const result = await load(idOrName);

  if (!result.ok) {
    return (
      <div className="flex flex-col gap-6">
        <BackToCatalog current="Pokémon" />
        <h1 className="text-3xl font-bold tracking-tight">Pokémon unavailable</h1>
        <ErrorState problem={result.problem} />
      </div>
    );
  }

  const { pokemon } = result;
  const name = formatName(pokemon.name);

  return (
    <article className="flex flex-col gap-8" aria-labelledby="pokemon-name">
      <BackToCatalog current={name} />

      <div className="grid gap-6 lg:grid-cols-2 lg:gap-10">
        <div className="flex items-center justify-center rounded-3xl bg-surface p-6">
          <Sprite src={pokemon.artwork} alt={`${name} official artwork`} size={320} priority />
        </div>

        <div className="flex flex-col gap-4">
          <div>
            <p className="text-lg font-semibold text-muted">{formatDexNumber(pokemon.id)}</p>
            <h1 id="pokemon-name" tabIndex={-1} className="text-4xl font-bold tracking-tight outline-none">
              {name}
            </h1>
          </div>

          <ul className="flex flex-wrap gap-2" aria-label="Types">
            {pokemon.types.map((type) => (
              <li key={type}>
                <TypeBadge type={type} />
              </li>
            ))}
          </ul>

          <dl className="grid grid-cols-2 gap-4 rounded-2xl bg-surface p-4 sm:max-w-sm">
            <div>
              <dt className="text-sm text-muted">Height</dt>
              <dd className="text-lg font-semibold">{formatMeters(pokemon.height)}</dd>
            </div>
            <div>
              <dt className="text-sm text-muted">Mass</dt>
              <dd className="text-lg font-semibold">{formatKg(pokemon.mass)}</dd>
            </div>
          </dl>

          {/* TODO(backend): render <AbilityList abilities={pokemon.abilities} /> here once
              `abilities` is added to PokemonDetailDto in docs/api/openapi.yaml. */}

          <div className="pt-2">
            <AddToCollectionButton pokedexId={pokemon.id} name={name} />
          </div>

          <section aria-labelledby="about-heading" className="flex flex-col gap-2 pt-2">
            <h2 id="about-heading" className="text-xl font-semibold">
              About
            </h2>
            <p className="max-w-prose leading-relaxed">
              {pokemon.description || "No description is available for this Pokémon yet."}
            </p>
          </section>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-2 lg:gap-10">
        <section aria-labelledby="stats-heading" className="flex flex-col gap-3 rounded-3xl bg-surface p-5">
          <h2 id="stats-heading" className="text-xl font-semibold">
            Base stats
          </h2>
          <StatMeters stats={pokemon.stats} />
        </section>

        <section aria-labelledby="evolution-heading" className="flex flex-col gap-3 rounded-3xl bg-surface p-5">
          <h2 id="evolution-heading" className="text-xl font-semibold">
            Evolution
          </h2>
          <EvolutionTree root={pokemon.evolution.root} currentId={pokemon.id} />
        </section>
      </div>
    </article>
  );
}

function BackToCatalog({ current }: { current: string }) {
  return (
    <Breadcrumbs>
      <Breadcrumbs.Item href="/pokemon">Catalog</Breadcrumbs.Item>
      <Breadcrumbs.Item>{current}</Breadcrumbs.Item>
    </Breadcrumbs>
  );
}
