import { AppLink } from "@/shared/ui/AppLink";
import { formatDexNumber } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { artworkForId, spriteForId } from "@/shared/lib/isSafeHttpUrl";
import { Sprite } from "@/shared/ui/Sprite";
import { triggerLabel } from "../lib/evolution";
import type { EvolutionNode, EvolutionTrigger } from "../types";

type Props = { root: EvolutionNode; currentId: number };

/**
 * Evolution tree (BR-04, docs/ui-ux.md §5.2): every stage links to its detail page;
 * branches (Eevee's 8) are kept and wrap into compact rows. Nested lists give screen
 * readers the tree structure.
 *
 * Layout follows the panel's width (container queries; the panel is `@container`): a linear
 * chain runs left-to-right once the panel is at least 32rem wide, otherwise top-to-bottom,
 * so it never overflows the half-width panel on desktop.
 */
export function EvolutionTree({ root, currentId }: Props) {
  if (root.evolvesTo.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 pt-3">
        <ul aria-label="Evolution chain">
          <li>
            <Stage node={root} currentId={currentId} />
          </li>
        </ul>
        <p className="text-sm text-muted">This Pokémon does not evolve.</p>
      </div>
    );
  }
  return (
    // pt-3: room for the "This Pokémon" badge, which overlaps the top edge of its card.
    <ul aria-label="Evolution chain" className="flex justify-center pt-3">
      <li>
        <Branch node={root} currentId={currentId} />
      </li>
    </ul>
  );
}

function Branch({ node, currentId }: { node: EvolutionNode; currentId: number }) {
  const children = node.evolvesTo;
  const isFork = children.length > 1;
  return (
    <div className={`flex items-center gap-2 ${isFork ? "flex-col" : "flex-col @lg:flex-row"}`}>
      <Stage node={node} currentId={currentId} />
      {children.length > 0 && (
        <ul
          className={
            isFork
              ? "flex flex-wrap justify-center gap-x-3 gap-y-4"
              : "flex flex-col items-center gap-2 @lg:flex-row"
          }
        >
          {children.map((child) => (
            <li
              key={child.speciesId}
              className={`flex items-center gap-1 ${isFork ? "flex-col" : "flex-col @lg:flex-row"}`}
            >
              {child.trigger && <Trigger trigger={child.trigger} inRow={!isFork} />}
              <Branch node={child} currentId={currentId} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/**
 * Arrow + the trigger in words. Fixed size so stages stay aligned: in a branch grid a
 * two-line trigger ("Use Thunder Stone") must not push its card below its neighbours.
 */
function Trigger({ trigger, inRow }: { trigger: EvolutionTrigger; inRow: boolean }) {
  return (
    <div
      className={`flex h-12 w-28 flex-col items-center justify-center gap-0.5 text-center text-xs leading-tight text-muted ${
        inRow ? "@lg:h-auto @lg:w-16" : ""
      }`}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className={`size-4 shrink-0 rotate-90 ${inRow ? "@lg:rotate-0" : ""}`}
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M5 12h14M13 6l6 6-6 6" />
      </svg>
      <span>
        <span className="sr-only">Evolves by: </span>
        <span>{triggerLabel(trigger)}</span>
      </span>
    </div>
  );
}

/**
 * One stage, all the same compact size: official artwork in a fixed 80 px box (falls back
 * to the sprite), number and name. The current stage is marked by an accent border and a
 * "This Pokémon" badge overlapping the top edge (no extra height), plus aria-current.
 */
function Stage({ node, currentId }: { node: EvolutionNode; currentId: number }) {
  const isCurrent = node.speciesId === currentId;
  const name = formatName(node.name);
  return (
    <AppLink
      href={`/pokemon/${node.speciesId}`}
      aria-current={isCurrent ? "page" : undefined}
      className={`relative flex w-28 cursor-pointer flex-col items-center gap-0.5 rounded-2xl border-2 bg-surface px-2 pt-3 pb-2 text-foreground no-underline transition hover:-translate-y-0.5 hover:no-underline hover:shadow-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus motion-reduce:transition-none motion-reduce:hover:translate-y-0 ${
        isCurrent ? "border-accent" : "border-border hover:border-accent"
      }`}
    >
      {isCurrent && (
        <span className="absolute -top-2.5 left-1/2 -translate-x-1/2 rounded-full bg-accent px-2 py-0.5 text-xs leading-tight font-semibold whitespace-nowrap text-accent-foreground">
          This Pokémon
        </span>
      )}
      <span className="flex size-20 items-center justify-center">
        <Sprite
          src={artworkForId(node.speciesId)}
          fallbackSrc={spriteForId(node.speciesId)}
          alt=""
          size={80}
          className="size-20"
        />
      </span>
      <span className="text-xs text-muted">{formatDexNumber(node.speciesId)}</span>
      <span className="max-w-full truncate text-sm font-semibold">{name}</span>
    </AppLink>
  );
}
