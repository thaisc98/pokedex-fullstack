/**
 * URL guard for anything we render as a link or image (docs/security.md §5).
 * Only absolute http(s) URLs without credentials pass, so `javascript:`,
 * `data:` and friends can never reach an `href` or `src`.
 */
export function isSafeHttpUrl(
  value: unknown,
  options: { allowedHosts?: readonly string[]; httpsOnly?: boolean } = {},
): value is string {
  if (typeof value !== "string" || value.length === 0) return false;
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return false;
  }
  const protocols = options.httpsOnly ? ["https:"] : ["http:", "https:"];
  if (!protocols.includes(url.protocol)) return false;
  if (url.username || url.password) return false;
  if (options.allowedHosts && !options.allowedHosts.includes(url.hostname)) return false;
  return true;
}

export const SPRITE_HOST = "raw.githubusercontent.com";
const SPRITE_PATH_PREFIX = "/PokeAPI/sprites/";

/** The backend's placeholder (domain `ImageUrl.PLACEHOLDER`). */
export const PLACEHOLDER_SPRITE =
  "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png";

/**
 * True only for HTTPS URLs inside the PokeAPI sprites repo, matching the
 * `next/image` `remotePatterns` allow-list in next.config.ts.
 */
export function isSpriteUrl(value: unknown): value is string {
  if (!isSafeHttpUrl(value, { allowedHosts: [SPRITE_HOST], httpsOnly: true })) return false;
  // `new URL` normalises `..` segments, so check the normalised path, and reject the raw form too.
  const { pathname } = new URL(value);
  return pathname.startsWith(SPRITE_PATH_PREFIX) && !value.includes("..");
}

/** The sprite URL if it is allow-listed, otherwise the placeholder. */
export function safeSprite(value: unknown): string {
  return isSpriteUrl(value) ? value : PLACEHOLDER_SPRITE;
}

/** Official artwork for a species id (the hero image's path), from the allow-listed base. */
export function artworkForId(id: number): string {
  return `https://${SPRITE_HOST}${SPRITE_PATH_PREFIX}master/sprites/pokemon/other/official-artwork/${Math.trunc(id)}.png`;
}

/** Front sprite for a species id, built from the allow-listed base (never from upstream URLs). */
export function spriteForId(id: number): string {
  return `https://${SPRITE_HOST}${SPRITE_PATH_PREFIX}master/sprites/pokemon/${Math.trunc(id)}.png`;
}
