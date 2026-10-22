import type { Metadata } from "next";
import { CatalogPagination, TopPager } from "@/features/catalog/components/CatalogPagination";
import { PokemonCard } from "@/features/catalog/components/PokemonCard";
import { PokemonSearch } from "@/features/catalog/components/PokemonSearch";
import { catalogHref, parseCatalogParams } from "@/features/catalog/lib/catalogParams";
import { CatalogError, getCatalogPage } from "@/server/catalogClient";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorState } from "@/shared/ui/ErrorState";

export const metadata: Metadata = { title: "Catalog" };

const numberFormat = new Intl.NumberFormat("en");

/** US01 catalog: Server Component, page + size from the URL (Next.js 16: searchParams is a Promise). */
export default async function CatalogPage(props: PageProps<"/pokemon">) {
  const params = parseCatalogParams(await props.searchParams);

  let data;
  try {
    data = await getCatalogPage(params);
  } catch (error) {
    if (!(error instanceof CatalogError)) throw error;
    return (
      <CatalogLayout>
        <ErrorState problem={error.problem} />
      </CatalogLayout>
    );
  }

  const { content, totalElements, totalPages, page, size } = data;
  const currentPage = page + 1;

  if (content.length === 0) {
    // BR-01: a page past the end is empty, not an error.
    return (
      <CatalogLayout>
        <EmptyState
          title="No Pokémon on this page"
          description={
            totalElements > 0
              ? `There are only ${numberFormat.format(totalPages)} pages.`
              : "The catalog is empty right now."
          }
          action={
            totalElements > 0 ? (
              <AppButtonLink href={catalogHref({ page: 1, size })}>Go to page 1</AppButtonLink>
            ) : undefined
          }
        />
      </CatalogLayout>
    );
  }

  const first = page * size + 1;
  const last = first + content.length - 1;

  return (
    <CatalogLayout
      summary={`Showing ${numberFormat.format(first)}–${numberFormat.format(last)} of ${numberFormat.format(totalElements)}`}
      pager={<TopPager page={currentPage} size={size} totalPages={totalPages} />}
    >
      {/* Heading order (h1 → h2 → card h3), for screen-reader navigation by headings. */}
      <h2 className="sr-only">Pokémon list</h2>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-4 xl:grid-cols-5">
        {content.map((pokemon, index) => (
          <li key={pokemon.id}>
            {/* First row is above the fold on every breakpoint. */}
            <PokemonCard pokemon={pokemon} priority={index < 5} />
          </li>
        ))}
      </ul>
      <CatalogPagination page={currentPage} size={size} totalPages={totalPages} />
    </CatalogLayout>
  );
}

function CatalogLayout({
  children,
  summary,
  pager,
}: {
  children: React.ReactNode;
  summary?: string;
  pager?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-3xl font-bold tracking-tight">Pokémon</h1>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <PokemonSearch />
        {(summary || pager) && (
          <div className="flex flex-wrap items-center justify-between gap-2 sm:justify-end">
            {summary && (
              <p className="text-sm text-muted" aria-live="polite">
                {summary}
              </p>
            )}
            {pager}
          </div>
        )}
      </div>
      {children}
    </div>
  );
}
