"use client";

import { Link } from "@heroui/react";
import { buttonVariants } from "@heroui/styles";
import NextLink from "next/link";
import type { ComponentProps, ReactNode, Ref } from "react";

type AppLinkProps = Omit<ComponentProps<typeof Link>, "href" | "render"> & {
  /** Internal path only, e.g. `/pokemon/25`. */
  href: string;
  prefetch?: boolean;
  "aria-current"?: "page" | undefined;
};

type ButtonLinkProps = {
  href: string;
  children: ReactNode;
  variant?: "primary" | "secondary" | "tertiary" | "outline" | "ghost";
  size?: "sm" | "md" | "lg";
  className?: string;
  /** Plain DOM click (this is `next/link`, not a React Aria component). */
  onClick?: () => void;
};

/**
 * Internal link that looks like a HeroUI Button (HeroUI's "Using with routing
 * libraries" pattern: the variant classes on `next/link`). Keeps link semantics.
 */
export function AppButtonLink({
  href,
  children,
  variant = "primary",
  size = "md",
  className = "",
  onClick,
}: ButtonLinkProps) {
  return (
    <NextLink href={href} onClick={onClick} className={`${buttonVariants({ variant, size })} ${className}`}>
      {children}
    </NextLink>
  );
}

/**
 * Internal link: HeroUI `Link` (styles, focus ring, React Aria press handling)
 * rendered as `next/link` (client navigation + prefetch). AGENTS.md: never a raw `<a href>`.
 */
export function AppLink({ href, prefetch, ...props }: AppLinkProps) {
  return (
    <Link
      {...props}
      href={href}
      // HeroUI types the render props for any element; Link always renders an anchor.
      render={({ ref, ...domProps }) => (
        <NextLink {...domProps} ref={ref as Ref<HTMLAnchorElement>} href={href} prefetch={prefetch} />
      )}
    />
  );
}
