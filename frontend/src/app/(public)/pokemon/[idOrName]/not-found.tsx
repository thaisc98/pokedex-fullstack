import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";

/** 404 (and malformed name, 400) for a Pokémon (ui-ux.md §4 US02, §11). */
export default function PokemonNotFound() {
  return (
    <EmptyState
      headingLevel={1}
      title="We couldn’t find that Pokémon"
      description="Check the name or Pokédex number and try again, or browse the catalog."
      action={<AppButtonLink href="/pokemon">Back to catalog</AppButtonLink>}
    />
  );
}
