"use client";

import { Card, Chip, Label, ListBox, Pagination, SearchField, Select, Skeleton, Table } from "@heroui/react";
import { useRouter, useSearchParams } from "next/navigation";
import { useMemo, useState } from "react";
import { pageItems } from "@/features/catalog/lib/catalogParams";
import type { CollectedPokemon } from "@/shared/api/types";
import { formatDexNumber } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { AppButtonLink, AppLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";
import { ErrorState } from "@/shared/ui/ErrorState";
import { Sprite } from "@/shared/ui/Sprite";
import { useListCollectionQuery } from "../collectionApi";
import { COLLECTION_PAGE_SIZE, collectionHref, parseCollectionParams, type CollectionParams } from "../lib/collectionParams";
import { RowActions } from "./RowActions";

const ALL_TAGS = "__all__";
const dateFormat = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" });

/** Collection (ui-ux.md §5.3): Table ≥ 1024 px, cards below; search, tag filter and page in the URL. */
export function CollectionList() {
  const router = useRouter();
  const params = parseCollectionParams(useSearchParams());
  const { data, error, isLoading, isFetching, refetch } = useListCollectionQuery({
    page: params.page,
    size: COLLECTION_PAGE_SIZE,
    q: params.q,
    tag: params.tag,
  });

  const go = (next: Partial<CollectionParams>) =>
    router.push(collectionHref({ ...params, page: 1, ...next }));

  // No "list tags" endpoint: offer the tags seen on this page plus the active one.
  const tagOptions = useMemo(() => {
    const tags = new Set<string>(data?.content.flatMap((item) => item.custom.tags) ?? []);
    if (params.tag) tags.add(params.tag);
    return [...tags].sort();
  }, [data, params.tag]);

  const filtered = Boolean(params.q || params.tag);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-3xl font-bold tracking-tight">My collection</h1>
        {data && (
          <p className="text-sm text-muted" aria-live="polite">
            {data.totalElements === 1 ? "1 record" : `${data.totalElements} records`}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <SearchBox key={params.q ?? ""} initial={params.q ?? ""} onSearch={(q) => go({ q: q || undefined })} />
        <Select
          className="sm:w-56"
          value={params.tag ?? ALL_TAGS}
          onChange={(value) => go({ tag: value && value !== ALL_TAGS ? String(value) : undefined })}
        >
          <Label>Tag</Label>
          <Select.Trigger>
            <Select.Value />
            <Select.Indicator />
          </Select.Trigger>
          <Select.Popover>
            <ListBox>
              <ListBox.Item id={ALL_TAGS} textValue="All tags">
                All tags
                <ListBox.ItemIndicator />
              </ListBox.Item>
              {tagOptions.map((tag) => (
                <ListBox.Item key={tag} id={tag} textValue={tag}>
                  {tag}
                  <ListBox.ItemIndicator />
                </ListBox.Item>
              ))}
            </ListBox>
          </Select.Popover>
        </Select>
      </div>

      {isLoading ? (
        <ListSkeleton />
      ) : error ? (
        <ErrorState problem={"problem" in error ? error.problem : undefined} onRetry={() => void refetch()} />
      ) : !data || data.content.length === 0 ? (
        filtered ? (
          <EmptyState
            title="No matching Pokémon"
            description="Nothing in your collection matches these filters."
            action={<AppButtonLink href="/collection">Clear filters</AppButtonLink>}
          />
        ) : params.page > 1 ? (
          <EmptyState
            title="No Pokémon on this page"
            action={<AppButtonLink href="/collection">Go to page 1</AppButtonLink>}
          />
        ) : (
          <EmptyState
            title="Your collection is empty"
            description="Browse the catalog and add your first Pokémon."
            action={<AppButtonLink href="/pokemon">Browse catalog</AppButtonLink>}
          />
        )
      ) : (
        <div className={`flex flex-col gap-6 transition-opacity ${isFetching ? "opacity-70" : ""}`} aria-busy={isFetching}>
          <CollectionTable items={data.content} />
          <CollectionCards items={data.content} />
          {data.totalPages > 1 && (
            <CollectionPagination page={params.page} totalPages={data.totalPages} onPage={(page) => go({ ...params, page })} />
          )}
        </div>
      )}
    </div>
  );
}

function SearchBox({ initial, onSearch }: { initial: string; onSearch: (q: string) => void }) {
  const [value, setValue] = useState(initial);
  return (
    <SearchField
      className="sm:w-72"
      value={value}
      onChange={setValue}
      onSubmit={(q) => onSearch(q.trim())}
      onClear={() => onSearch("")}
    >
      <Label>Search by name</Label>
      <SearchField.Group>
        <SearchField.SearchIcon />
        <SearchField.Input placeholder="e.g. pikachu" maxLength={50} />
        <SearchField.ClearButton />
      </SearchField.Group>
    </SearchField>
  );
}

function localName(item: CollectedPokemon) {
  return item.custom.localizedNames.map((n) => n.value).join(" · ") || "—";
}

function TagList({ tags }: { tags: string[] }) {
  if (tags.length === 0) return <span className="text-muted">—</span>;
  return (
    <ul className="flex flex-wrap gap-1" aria-label="Tags">
      {tags.map((tag) => (
        <li key={tag}>
          <Chip size="sm">{tag}</Chip>
        </li>
      ))}
    </ul>
  );
}

function CollectionTable({ items }: { items: CollectedPokemon[] }) {
  return (
    <div className="hidden lg:block">
      <Table>
        <Table.ScrollContainer>
          <Table.Content aria-label="Collected Pokémon">
            <Table.Header>
              <Table.Column>#</Table.Column>
              <Table.Column isRowHeader>Name</Table.Column>
              <Table.Column>Local names</Table.Column>
              <Table.Column>Region</Table.Column>
              <Table.Column>Tags</Table.Column>
              <Table.Column>Synced</Table.Column>
              <Table.Column>
                <span className="sr-only">Actions</span>
              </Table.Column>
            </Table.Header>
            <Table.Body>
              {items.map((item) => (
                <Table.Row key={item.id} id={item.id}>
                  <Table.Cell className="tabular-nums text-muted">{formatDexNumber(item.id)}</Table.Cell>
                  <Table.Cell>
                    <AppLink href={`/collection/${item.id}`} className="flex items-center gap-2 font-medium text-foreground">
                      <Sprite src={item.snapshot.sprite} alt="" size={40} className="[image-rendering:pixelated]" />
                      {formatName(item.snapshot.name)}
                    </AppLink>
                  </Table.Cell>
                  <Table.Cell>{localName(item)}</Table.Cell>
                  <Table.Cell>{item.custom.geo?.region || "—"}</Table.Cell>
                  <Table.Cell>
                    <TagList tags={item.custom.tags} />
                  </Table.Cell>
                  <Table.Cell className="text-sm text-muted">{dateFormat.format(new Date(item.syncedAt))}</Table.Cell>
                  <Table.Cell>
                    <RowActions pokedexId={item.id} name={item.snapshot.name} />
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Content>
        </Table.ScrollContainer>
      </Table>
    </div>
  );
}

function CollectionCards({ items }: { items: CollectedPokemon[] }) {
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:hidden" aria-label="Collected Pokémon">
      {items.map((item) => (
        <li key={item.id}>
          <Card className="h-full">
            <Card.Header className="flex-row items-start justify-between gap-2">
              <div className="flex items-center gap-3">
                <Sprite src={item.snapshot.sprite} alt="" size={56} className="[image-rendering:pixelated]" />
                <div className="flex flex-col">
                  <span className="text-xs text-muted">{formatDexNumber(item.id)}</span>
                  <AppLink href={`/collection/${item.id}`} className="font-semibold text-foreground">
                    {formatName(item.snapshot.name)}
                  </AppLink>
                  <span className="text-sm text-muted">
                    {[localName(item), item.custom.geo?.region].filter((v) => v && v !== "—").join(" · ") || "No custom names yet"}
                  </span>
                </div>
              </div>
              <RowActions pokedexId={item.id} name={item.snapshot.name} />
            </Card.Header>
            <Card.Content className="flex flex-col gap-2">
              <TagList tags={item.custom.tags} />
              <p className="text-xs text-muted">Synced {dateFormat.format(new Date(item.syncedAt))}</p>
            </Card.Content>
          </Card>
        </li>
      ))}
    </ul>
  );
}

function CollectionPagination({
  page,
  totalPages,
  onPage,
}: {
  page: number;
  totalPages: number;
  onPage: (page: number) => void;
}) {
  return (
    <Pagination aria-label="Collection pages" className="justify-center">
      <Pagination.Content>
        <Pagination.Item>
          <Pagination.Previous isDisabled={page <= 1} onPress={() => onPage(page - 1)} aria-label="Previous page">
            <Pagination.PreviousIcon />
            <span>Prev</span>
          </Pagination.Previous>
        </Pagination.Item>
        {pageItems(page, totalPages).map((item) =>
          typeof item === "number" ? (
            <Pagination.Item key={item}>
              <Pagination.Link
                isActive={item === page}
                onPress={() => onPage(item)}
                aria-label={item === page ? `Page ${item}, current page` : `Page ${item}`}
              >
                {item}
              </Pagination.Link>
            </Pagination.Item>
          ) : (
            <Pagination.Item key={item}>
              <Pagination.Ellipsis />
            </Pagination.Item>
          ),
        )}
        <Pagination.Item>
          <Pagination.Next isDisabled={page >= totalPages} onPress={() => onPage(page + 1)} aria-label="Next page">
            <span>Next</span>
            <Pagination.NextIcon />
          </Pagination.Next>
        </Pagination.Item>
      </Pagination.Content>
    </Pagination>
  );
}

export function ListSkeleton() {
  return (
    <div className="flex flex-col gap-3" aria-busy="true">
      <span className="sr-only" role="status">
        Loading your collection…
      </span>
      {Array.from({ length: 6 }, (_, i) => (
        <Skeleton key={i} className="h-16 w-full rounded-2xl" />
      ))}
    </div>
  );
}
