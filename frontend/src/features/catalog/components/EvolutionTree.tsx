import { AppLink } from "@/shared/ui/AppLink";
import { formatDexNumber } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { spriteForId } from "@/shared/lib/isSafeHttpUrl";
import { Sprite } from "@/shared/ui/Sprite";
import { triggerLabel } from "../lib/evolution";
import type { EvolutionNode, EvolutionTrigger } from "../types";

type Props = { root: EvolutionNode; currentId: number };

/**
 * Evolution tree (BR-04, docs/ui-ux.md §5.2): every stage links to its detail
 * page; branches (Eevee's 8) are kept and shown as a wrapping grid. The chain is
 * centred in its panel. Nested lists give screen readers the tree structure.
 */
export function EvolutionTree({ root, currentId }: Props) {
  if (root.evolvesTo.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3">
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
    <ul aria-label="Evolution chain" className="flex justify-center">
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
    <div className={`flex items-center gap-3 ${isFork ? "flex-col" : "flex-col md:flex-row"}`}>
      <Stage node={node} currentId={currentId} />
      {children.length > 0 && (
        <ul
          className={
            isFork
              ? "grid grid-cols-2 gap-3 sm:grid-cols-4"
              : "flex flex-col items-center gap-3 md:flex-row"
          }
        >
          {children.map((child) => (
            <li
              key={child.speciesId}
              className={`flex items-center gap-2 ${isFork ? "flex-col" : "flex-col md:flex-row"}`}
            >
              {child.trigger && <Trigger trigger={child.trigger} vertical={isFork} />}
              <Branch node={child} currentId={currentId} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Trigger({ trigger, vertical }: { trigger: EvolutionTrigger; vertical: boolean }) {
  return (
    <div
      className={`flex w-24 flex-col items-center gap-0.5 text-center text-xs text-muted ${vertical ? "min-h-12" : ""}`}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className={`size-5 ${vertical ? "rotate-90" : "rotate-90 md:rotate-0"}`}
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
 * One stage: every card has the same size. The sprite sits in a fixed frame and is scaled up
 * crisply (like the catalog cards), so small and large Pokémon look alike. The current stage
 * gets a 2 px accent border plus a "This Pokémon" badge laid over the top of the card, so it
 * is marked by more than color without changing the card's height.
 */
function Stage({ node, currentId }: { node: EvolutionNode; currentId: number }) {
  const isCurrent = node.speciesId === currentId;
  const name = formatName(node.name);
  return (
    <AppLink
      href={`/pokemon/${node.speciesId}`}
      aria-current={isCurrent ? "page" : undefined}
      className={`relative flex w-32 flex-col items-center gap-1 rounded-2xl px-2 pt-6 pb-3 text-foreground no-underline transition-colors hover:bg-surface-secondary hover:no-underline sm:w-36 ${
        isCurrent ? "border-2 border-accent bg-surface-secondary" : "border-2 border-border bg-surface"
      }`}
    >
      {isCurrent && (
        <span className="absolute top-1 left-1/2 -translate-x-1/2 rounded-full bg-accent px-2 py-0.5 text-xs leading-tight font-semibold whitespace-nowrap text-accent-foreground">
          This Pokémon
        </span>
      )}
      <span className="flex size-24 items-center justify-center sm:size-28">
        <Sprite
          src={spriteForId(node.speciesId)}
          alt=""
          size={96}
          // A large width so the optimizer returns the original pixels (it never enlarges).
          sizes="128px"
          className="size-24 sm:size-28 [image-rendering:pixelated]"
        />
      </span>
      <span className="text-xs text-muted">{formatDexNumber(node.speciesId)}</span>
      <span className="max-w-full truncate text-sm font-semibold">{name}</span>
    </AppLink>
  );
}
