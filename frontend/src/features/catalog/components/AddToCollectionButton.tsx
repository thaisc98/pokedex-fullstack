"use client";

import { Tooltip } from "@heroui/react";
import { buttonVariants } from "@heroui/styles";

/**
 * Phase A placeholder for US03. Shown disabled with the reason ("Sign in to add").
 * The trigger itself is the (aria-disabled) button so it stays focusable and the
 * tooltip is reachable by keyboard; a natively disabled button can't receive focus.
 * Phase B wires sign-in + POST /collection.
 */
export function AddToCollectionButton() {
  return (
    <div className="flex flex-col items-start gap-1.5">
      <Tooltip delay={0}>
        <Tooltip.Trigger
          aria-disabled="true"
          aria-describedby="add-to-collection-hint"
          className={`${buttonVariants({ variant: "secondary" })} inline-flex h-11 cursor-not-allowed flex-row items-center gap-2 px-5 whitespace-nowrap`}
        >
          <svg
            aria-hidden="true"
            viewBox="0 0 24 24"
            className="size-4"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <path d="M12 5v14M5 12h14" />
          </svg>
          Add to collection
        </Tooltip.Trigger>
        <Tooltip.Content showArrow>
          <Tooltip.Arrow />
          Sign in to add
        </Tooltip.Content>
      </Tooltip>
      {/* Visible too, so the reason never depends on hovering. */}
      <p id="add-to-collection-hint" className="text-xs text-muted">
        Sign in to add this Pokémon to the collection.
      </p>
    </div>
  );
}
