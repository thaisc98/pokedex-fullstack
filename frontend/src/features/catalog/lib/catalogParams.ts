/**
 * Catalog URL state (docs/ui-ux.md P4 "the URL is the state").
 * The URL uses a human, 1-based `?page=`; the API is 0-based (BR-01), so
 * `catalogClient` subtracts one.
 */
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100] as const;
export const MAX_PAGE_SIZE = 100;

export type CatalogParams = { page: number; size: number };
type RawParams = Record<string, string | string[] | undefined>;

function positiveInt(raw: string | string[] | undefined): number | undefined {
  const value = Array.isArray(raw) ? raw[0] : raw;
  if (value === undefined || !/^\d+$/.test(value)) return undefined;
  const n = Number(value);
  return Number.isSafeInteger(n) && n > 0 ? n : undefined;
}

export function parseCatalogParams(raw: RawParams): CatalogParams {
  const page = positiveInt(raw.page) ?? 1;
  const size = positiveInt(raw.size);
  return { page, size: size !== undefined && size <= MAX_PAGE_SIZE ? size : DEFAULT_PAGE_SIZE };
}

export function catalogHref({ page, size }: CatalogParams): string {
  const query = new URLSearchParams();
  if (page > 1) query.set("page", String(page));
  if (size !== DEFAULT_PAGE_SIZE) query.set("size", String(size));
  const qs = query.toString();
  return qs ? `/pokemon?${qs}` : "/pokemon";
}

export type PageItem = number | "ellipsis-start" | "ellipsis-end";

/** Page numbers to show: first, last, current ± 1, with ellipses for gaps of 2 or more. */
export function pageItems(current: number, total: number): PageItem[] {
  if (total <= 0) return [];
  const pages = new Set([1, total, current - 1, current, current + 1]);
  // Show a single skipped page instead of an ellipsis that hides only one number.
  if (current - 1 === 3) pages.add(2);
  if (current + 1 === total - 2) pages.add(total - 1);
  if (current === 1) pages.add(2);
  if (current === total) pages.add(total - 1);
  const sorted = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b);
  const items: PageItem[] = [];
  sorted.forEach((p, i) => {
    const prev = sorted[i - 1];
    if (prev !== undefined && p - prev > 1) items.push(p < current ? "ellipsis-start" : "ellipsis-end");
    items.push(p);
  });
  return items;
}
