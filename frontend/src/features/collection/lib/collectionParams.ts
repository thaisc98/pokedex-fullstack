/** Collection URL state (ui-ux.md P4): `?page=` (1-based), `?q=` (name search), `?tag=`. */
export const COLLECTION_PAGE_SIZE = 20;

export type CollectionParams = { page: number; q?: string; tag?: string };

const TAG = /^[a-z0-9-]{1,30}$/;

export function parseCollectionParams(search: URLSearchParams): CollectionParams {
  const rawPage = search.get("page");
  const page = rawPage && /^\d+$/.test(rawPage) && Number(rawPage) > 0 ? Number(rawPage) : 1;
  const q = search.get("q")?.trim().slice(0, 50) || undefined;
  const rawTag = search.get("tag")?.trim().toLowerCase();
  const tag = rawTag && TAG.test(rawTag) ? rawTag : undefined;
  return { page, q, tag };
}

export function collectionHref({ page, q, tag }: CollectionParams): string {
  const query = new URLSearchParams();
  if (q) query.set("q", q);
  if (tag) query.set("tag", tag);
  if (page > 1) query.set("page", String(page));
  const qs = query.toString();
  return qs ? `/collection?${qs}` : "/collection";
}
