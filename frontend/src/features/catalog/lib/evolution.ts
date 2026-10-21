import { formatSlug } from "@/shared/lib/formatName";
import type { EvolutionTrigger } from "../types";

/** The evolution trigger in words (docs/ui-ux.md §5.2), e.g. "Level 16", "Use Thunder Stone", "Trade". */
export function triggerLabel(trigger: EvolutionTrigger): string {
  switch (trigger.kind) {
    case "level-up":
      return trigger.minLevel ? `Level ${trigger.minLevel}` : "Level up";
    case "use-item":
      return `Use ${formatSlug(trigger.item)}`;
    case "trade":
      return "Trade";
    case "other":
      return formatSlug(trigger.name);
  }
}
