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
 * page; branches (Eevee's 8) are kept and shown as a wrapping grid.
 * Nested lists give screen readers the tree structure.
 */
export function EvolutionTree({ root, currentId }: Props) {
  if (root.evolvesTo.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3">
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
    <ul aria-label="Evolution chain" className="flex justify-center md:justify-start">
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
      className={`flex flex-col items-center gap-0.5 text-center text-xs text-muted ${vertical ? "min-h-12" : ""}`}
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

function Stage({ node, currentId }: { node: EvolutionNode; currentId: number }) {
  const isCurrent = node.speciesId === currentId;
  const name = formatName(node.name);
  return (
    <AppLink
      href={`/pokemon/${node.speciesId}`}
      aria-current={isCurrent ? "page" : undefined}
      className={`flex w-28 flex-col items-center gap-1 rounded-xl border p-2 text-foreground no-underline transition-colors hover:bg-surface-secondary hover:no-underline ${
        // Current stage: thicker accent border + a visible label (not color alone). The fill stays
        // a neutral surface so the muted number keeps >= 4.5:1 (an accent tint dropped it to 3.8:1).
        isCurrent ? "border-2 border-accent bg-surface-secondary" : "border-border bg-surface"
      }`}
    >
      <Sprite src={spriteForId(node.speciesId)} alt="" size={72} />
      <span className="text-xs text-muted">{formatDexNumber(node.speciesId)}</span>
      <span className="text-sm font-semibold">{name}</span>
      {isCurrent && <span className="text-xs font-medium text-foreground">This Pokémon</span>}
    </AppLink>
  );
}
