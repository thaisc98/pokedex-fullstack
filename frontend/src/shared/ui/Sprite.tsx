"use client";
// Client component on purpose: `style={{ color: undefined }}` below must reach next/image
// as-is. Passed across the server→client boundary, the `undefined` key would be dropped.

import Image from "next/image";
import { useState } from "react";
import { safeSprite } from "@/shared/lib/isSafeHttpUrl";

type SpriteProps = {
  src: string;
  /** Shown instead when `src` fails to load (e.g. no official artwork for a form). */
  fallbackSrc?: string;
  alt: string;
  size: number;
  /** `sizes` for next/image; defaults to the intrinsic size. */
  sizes?: string;
  priority?: boolean;
  /** Load immediately without a preload hint (small list thumbnails that are on screen). */
  eager?: boolean;
  className?: string;
};

/**
 * Pokémon image via `next/image`. The URL is checked against the sprite
 * allow-list first (security.md §5); anything else becomes the placeholder.
 * Below-the-fold images lazy-load (next/image default) unless `priority`.
 */
export function Sprite({
  src,
  fallbackSrc,
  alt,
  size,
  sizes,
  priority = false,
  eager = false,
  className = "",
}: SpriteProps) {
  const [failed, setFailed] = useState(false);
  const current = failed && fallbackSrc ? fallbackSrc : src;
  return (
    <Image
      src={safeSprite(current)}
      onError={fallbackSrc && !failed ? () => setFailed(true) : undefined}
      alt={alt}
      width={size}
      height={size}
      priority={priority}
      loading={priority ? undefined : eager ? "eager" : "lazy"}
      sizes={sizes ?? `${size}px`}
      // next/image adds `style="color:transparent"` (hides alt text while loading). Inline style
      // attributes are blocked by our CSP (style-src has no 'unsafe-inline'), so we unset it here
      // and get the same effect from the `text-transparent` class.
      style={{ color: undefined }}
      className={`object-contain text-transparent ${className}`}
    />
  );
}
