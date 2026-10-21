"use client";
// Client component on purpose: `style={{ color: undefined }}` below must reach next/image
// as-is. Passed across the server→client boundary, the `undefined` key would be dropped.

import Image from "next/image";
import { safeSprite } from "@/shared/lib/isSafeHttpUrl";

type SpriteProps = {
  src: string;
  alt: string;
  size: number;
  priority?: boolean;
  className?: string;
};

/**
 * Pokémon image via `next/image`. The URL is checked against the sprite
 * allow-list first (security.md §5); anything else becomes the placeholder.
 * Below-the-fold images lazy-load (next/image default) unless `priority`.
 */
export function Sprite({ src, alt, size, priority = false, className = "" }: SpriteProps) {
  return (
    <Image
      src={safeSprite(src)}
      alt={alt}
      width={size}
      height={size}
      priority={priority}
      sizes={`${size}px`}
      // next/image adds `style="color:transparent"` (hides alt text while loading). Inline style
      // attributes are blocked by our CSP (style-src has no 'unsafe-inline'), so we unset it here
      // and get the same effect from the `text-transparent` class.
      style={{ color: undefined }}
      className={`object-contain text-transparent ${className}`}
    />
  );
}
