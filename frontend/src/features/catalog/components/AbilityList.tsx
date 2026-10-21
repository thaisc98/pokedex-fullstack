import { formatSlug } from "@/shared/lib/formatName";
import type { Ability } from "../types";

/** Abilities ("skills"); a hidden ability is italic AND labelled in text, never by color alone. */
export function AbilityList({ abilities, className = "" }: { abilities: Ability[]; className?: string }) {
  if (abilities.length === 0) return <p className={`text-muted ${className}`}>No abilities listed</p>;
  return (
    <ul className={`flex flex-col gap-0.5 ${className}`}>
      {abilities.map((ability) => (
        <li key={ability.name}>
          {ability.hidden ? (
            <>
              <span className="italic">{formatSlug(ability.name)}</span>{" "}
              <span className="text-xs text-muted">
                (hidden<span className="sr-only"> ability</span>)
              </span>
            </>
          ) : (
            formatSlug(ability.name)
          )}
        </li>
      ))}
    </ul>
  );
}
