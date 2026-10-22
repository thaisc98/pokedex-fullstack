"use client";

import { Breadcrumbs, Button, Chip, Skeleton, toast } from "@heroui/react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { selectIsAdmin } from "@/features/auth/authSlice";
import { AbilityList } from "@/features/catalog/components/AbilityList";
import { StatMeters } from "@/features/catalog/components/StatMeters";
import { isApiError } from "@/shared/api/apiError";
import type { CollectedPokemon } from "@/shared/api/types";
import { formatDexNumber, formatKg } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { problemCopy } from "@/shared/lib/problemCopy";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorState } from "@/shared/ui/ErrorState";
import { Sprite } from "@/shared/ui/Sprite";
import { useAppSelector } from "@/store/hooks";
import { useGetCollectedQuery, useSyncMutation } from "../collectionApi";
import { DeleteDialog } from "./DeleteDialog";
import { EditForm } from "./EditForm";

const dateFormat = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" });

/** `/collection/[pokedexId]`: read view + Edit (US04). Edit mode lives in the URL (`?edit=1`). */
export function CollectedDetail() {
  const params = useParams<{ pokedexId: string }>();
  const pokedexId = /^\d+$/.test(params.pokedexId) ? Number(params.pokedexId) : NaN;
  const router = useRouter();
  const editing = useSearchParams().get("edit") === "1";
  const isAdmin = useAppSelector(selectIsAdmin);
  const [removed, setRemoved] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [sync, { isLoading: syncing }] = useSyncMutation();
  const valid = Number.isInteger(pokedexId) && pokedexId > 0;
  const { data, error, isLoading, refetch } = useGetCollectedQuery(pokedexId, { skip: !valid || removed });

  const base = `/collection/${pokedexId}`;

  if (!valid || removed || (error && "status" in error && error.status === 404)) {
    return (
      <EmptyState
        headingLevel={1}
        title={removed ? "This record was removed" : "Not in your collection"}
        description={
          removed
            ? "Someone deleted it while you were editing."
            : "This Pokémon isn’t in the collection. Add it from its catalog page."
        }
        action={<AppButtonLink href="/collection">Back to collection</AppButtonLink>}
      />
    );
  }
  if (isLoading || !data) {
    if (error) {
      return <ErrorState problem={"problem" in error ? error.problem : undefined} onRetry={() => void refetch()} />;
    }
    return <DetailSkeleton />;
  }

  const name = formatName(data.snapshot.name);

  const onRefresh = async () => {
    try {
      await sync(pokedexId).unwrap();
      toast.success("Refreshed from PokeAPI", { description: "Your custom fields were kept." });
    } catch (e) {
      toast.danger("Couldn’t refresh", { description: isApiError(e) ? problemCopy(e.problem).message : undefined });
    }
  };

  return (
    <article className="flex flex-col gap-6" aria-labelledby="collected-name">
      <Breadcrumbs>
        <Breadcrumbs.Item href="/collection">Collection</Breadcrumbs.Item>
        <Breadcrumbs.Item>{name}</Breadcrumbs.Item>
      </Breadcrumbs>

      <header className="flex flex-wrap items-center gap-4">
        <div className="rounded-2xl bg-surface p-2">
          <Sprite src={data.snapshot.sprite} alt={`${name} sprite`} size={96} priority className="[image-rendering:pixelated]" />
        </div>
        <div className="flex flex-1 flex-col">
          <span className="text-sm text-muted">{formatDexNumber(data.id)}</span>
          <h1 id="collected-name" className="text-3xl font-bold tracking-tight">
            {name}
          </h1>
          <span className="text-sm text-muted">{data.snapshot.category}</span>
        </div>
        {!editing && (
          <div className="flex flex-wrap gap-2">
            <Button onPress={() => router.push(`${base}?edit=1`)}>Edit</Button>
            <Button variant="secondary" onPress={onRefresh} isPending={syncing} isDisabled={syncing}>
              Refresh from PokeAPI
            </Button>
            {isAdmin && (
              <Button variant="danger-soft" onPress={() => setDeleteOpen(true)}>
                Delete
              </Button>
            )}
          </div>
        )}
      </header>

      {editing ? (
        <section className="rounded-3xl bg-surface p-5">
          <EditForm
            key={`${formKey}-${data.version}`}
            item={data}
            onDone={() => router.push(base)}
            onCancel={() => router.push(base)}
            onReloadLatest={async () => {
              await refetch();
              setFormKey((k) => k + 1);
            }}
            onRemoved={() => setRemoved(true)}
          />
        </section>
      ) : (
        <ReadView item={data} />
      )}

      {isAdmin && (
        <DeleteDialog
          pokedexId={data.id}
          name={data.snapshot.name}
          isOpen={deleteOpen}
          onOpenChange={setDeleteOpen}
          onDeleted={() => router.push("/collection")}
        />
      )}
    </article>
  );
}

function ReadView({ item }: { item: CollectedPokemon }) {
  const { custom, snapshot } = item;
  const coords = custom.geo?.coordinates;
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section aria-labelledby="custom-heading" className="flex flex-col gap-4 rounded-3xl bg-surface p-5">
        <h2 id="custom-heading" className="text-xl font-semibold">
          Our notes
        </h2>
        {/* A <dl> may only hold dt/dd or one wrapping div per dt+dd group, so the 2-column
            row is made with the grid on the dl itself (col-span-2 for full-width groups). */}
        <dl className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <dt className="text-sm text-muted">Localized names</dt>
            <dd>
              {custom.localizedNames.length === 0 ? (
                <span className="text-muted">None yet</span>
              ) : (
                <ul className="flex flex-col gap-1">
                  {custom.localizedNames.map((n) => (
                    <li key={n.lang}>
                      <span className="font-medium" lang={n.lang}>
                        {n.value}
                      </span>{" "}
                      <span className="text-xs text-muted">({n.lang})</span>
                    </li>
                  ))}
                </ul>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-muted">Region</dt>
            <dd>{custom.geo?.region || <span className="text-muted">—</span>}</dd>
          </div>
          <div>
            <dt className="text-sm text-muted">Habitat</dt>
            <dd>{custom.geo?.habitat || <span className="text-muted">—</span>}</dd>
          </div>
          <div className="col-span-2">
            <dt className="text-sm text-muted">Coordinates</dt>
            <dd>{coords ? `${coords.latitude}, ${coords.longitude}` : <span className="text-muted">—</span>}</dd>
          </div>
          <div className="col-span-2">
            <dt className="text-sm text-muted">Tags</dt>
            <dd>
              {custom.tags.length === 0 ? (
                <span className="text-muted">None yet</span>
              ) : (
                <ul className="flex flex-wrap gap-1">
                  {custom.tags.map((t) => (
                    <li key={t}>
                      <Chip size="sm">{t}</Chip>
                    </li>
                  ))}
                </ul>
              )}
            </dd>
          </div>
        </dl>
      </section>

      <section aria-labelledby="upstream-heading" className="flex flex-col gap-4 rounded-3xl bg-surface p-5">
        <h2 id="upstream-heading" className="text-xl font-semibold">
          From PokeAPI <span className="text-sm font-normal text-muted">(read-only)</span>
        </h2>
        <dl className="grid grid-cols-2 gap-4">
          <div>
            <dt className="text-sm text-muted">Mass</dt>
            <dd>{formatKg(snapshot.mass)}</dd>
          </div>
          <div>
            <dt className="text-sm text-muted">Abilities</dt>
            <dd>
              <AbilityList abilities={snapshot.abilities} className="text-sm" />
            </dd>
          </div>
        </dl>
        <StatMeters stats={snapshot.stats} />
        <p className="text-xs text-muted">
          Synced {dateFormat.format(new Date(item.syncedAt))} · Notes updated {dateFormat.format(new Date(item.updatedAt))} ·
          Version {item.version}
        </p>
      </section>
    </div>
  );
}

export function DetailSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy="true">
      <span className="sr-only" role="status">
        Loading…
      </span>
      <Skeleton className="h-4 w-48 rounded-lg" />
      <div className="flex items-center gap-4">
        <Skeleton className="size-28 rounded-2xl" />
        <div className="flex flex-col gap-2">
          <Skeleton className="h-4 w-16 rounded-lg" />
          <Skeleton className="h-8 w-48 rounded-lg" />
        </div>
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-64 rounded-3xl" />
        <Skeleton className="h-64 rounded-3xl" />
      </div>
    </div>
  );
}
