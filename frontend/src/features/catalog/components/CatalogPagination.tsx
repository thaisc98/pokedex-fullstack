"use client";

import { Button, Label, ListBox, Pagination, Select, Skeleton } from "@heroui/react";
import { useRouter } from "next/navigation";
import { useEffect, useTransition } from "react";
import { useIsClient } from "@/shared/lib/useIsClient";
import { catalogHref, PAGE_SIZE_OPTIONS, pageItems } from "../lib/catalogParams";

type Props = { page: number; size: number; totalPages: number };

/**
 * Catalog pagination (ui-ux.md §5.1/§6.4): HeroUI Pagination + page-size Select.
 * Every change is a URL change (`?page=&size=`), so Back/Forward and sharing work.
 * Keyboard: Tab to a page, Enter/Space to go (React Aria buttons, aria-current on the active page).
 */
export function CatalogPagination({ page, size, totalPages }: Props) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const isClient = useIsClient();
  const go = (target: number, targetSize = size) =>
    startTransition(() => router.push(catalogHref({ page: target, size: targetSize })));

  // P6: prefetch the next page in the background.
  useEffect(() => {
    if (page < totalPages) router.prefetch(catalogHref({ page: page + 1, size }));
  }, [router, page, size, totalPages]);

  const hasPrev = page > 1;
  const hasNext = page < totalPages;

  return (
    <div
      className="flex flex-col items-center gap-4 sm:flex-row sm:justify-between"
      aria-busy={isPending || undefined}
    >
      <Pagination aria-label="Catalog pages" className="justify-center">
        <Pagination.Content>
          <Pagination.Item>
            <Pagination.Previous isDisabled={!hasPrev} onPress={() => go(page - 1)} aria-label="Previous page">
              <Pagination.PreviousIcon />
              <span>Prev</span>
            </Pagination.Previous>
          </Pagination.Item>
          {pageItems(page, totalPages).map((item) =>
            typeof item === "number" ? (
              <Pagination.Item key={item} className="hidden sm:list-item">
                <Pagination.Link
                  isActive={item === page}
                  onPress={() => go(item)}
                  aria-label={item === page ? `Page ${item}, current page` : `Page ${item}`}
                >
                  {item}
                </Pagination.Link>
              </Pagination.Item>
            ) : (
              <Pagination.Item key={item} className="hidden sm:list-item">
                <Pagination.Ellipsis />
              </Pagination.Item>
            ),
          )}
          <Pagination.Item className="sm:hidden">
            <span className="px-2 text-sm tabular-nums" aria-current="page">
              {page}/{totalPages}
            </span>
          </Pagination.Item>
          <Pagination.Item>
            <Pagination.Next isDisabled={!hasNext} onPress={() => go(page + 1)} aria-label="Next page">
              <span>Next</span>
              <Pagination.NextIcon />
            </Pagination.Next>
          </Pagination.Item>
        </Pagination.Content>
      </Pagination>

      {/*
        Rendered after hydration only: React Aria's hidden native <select> is visually hidden
        with an inline style attribute, which our CSP (no 'unsafe-inline' styles) blocks in
        server HTML. Client-created styles go through the CSSOM and are allowed.
      */}
      {isClient ? (
        <PageSizeSelect size={size} onChange={(next) => go(1, next)} />
      ) : (
        <Skeleton className="h-16 w-40 rounded-xl" />
      )}
    </div>
  );
}

function PageSizeSelect({ size, onChange }: { size: number; onChange: (size: number) => void }) {
  return (
    <Select
      className="w-40"
      value={String(size)}
      onChange={(value) => {
        if (value != null) onChange(Number(value));
      }}
    >
      <Label>Per page</Label>
      <Select.Trigger>
        <Select.Value />
        <Select.Indicator />
      </Select.Trigger>
      <Select.Popover>
        <ListBox>
          {PAGE_SIZE_OPTIONS.map((option) => (
            <ListBox.Item key={option} id={String(option)} textValue={`${option} per page`}>
              {option}
              <ListBox.ItemIndicator />
            </ListBox.Item>
          ))}
        </ListBox>
      </Select.Popover>
    </Select>
  );
}

/**
 * Compact pager next to "Showing …" at the top of the grid: Prev · X / Y · Next.
 * Deliberately not a list and not labelled "Next page": the full pagination below keeps
 * those names, so there is exactly one "Next page" control on the page.
 */
export function TopPager({ page, size, totalPages }: Props) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const go = (target: number) => startTransition(() => router.push(catalogHref({ page: target, size })));
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Catalog pages, top" className="flex items-center gap-1" aria-busy={isPending || undefined}>
      <Button
        size="sm"
        variant="ghost"
        isDisabled={page <= 1}
        onPress={() => go(page - 1)}
        aria-label={page > 1 ? `Prev: page ${page - 1}` : "Prev"}
      >
        ‹ Prev
      </Button>
      <span className="px-1 text-sm tabular-nums text-muted">
        {page} / {totalPages}
      </span>
      <Button
        size="sm"
        variant="ghost"
        isDisabled={page >= totalPages}
        onPress={() => go(page + 1)}
        aria-label={page < totalPages ? `Next: page ${page + 1}` : "Next"}
      >
        Next ›
      </Button>
    </nav>
  );
}
