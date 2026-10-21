/**
 * API slugs → display names (docs/ui-ux.md §8). PokeAPI names are lowercase
 * slugs; most become Title Case words, but some species need a hand-written
 * spelling.
 */
const NAME_EXCEPTIONS: Readonly<Record<string, string>> = {
  "mr-mime": "Mr. Mime",
  "mr-mime-galar": "Mr. Mime (Galar)",
  "mr-rime": "Mr. Rime",
  "mime-jr": "Mime Jr.",
  "ho-oh": "Ho-Oh",
  "porygon-z": "Porygon-Z",
  porygon2: "Porygon2",
  "nidoran-f": "Nidoran♀",
  "nidoran-m": "Nidoran♂",
  farfetchd: "Farfetch’d",
  "farfetchd-galar": "Farfetch’d (Galar)",
  sirfetchd: "Sirfetch’d",
  "type-null": "Type: Null",
  "jangmo-o": "Jangmo-o",
  "hakamo-o": "Hakamo-o",
  "kommo-o": "Kommo-o",
  flabebe: "Flabébé",
  "chi-yu": "Chi-Yu",
  "chien-pao": "Chien-Pao",
  "ting-lu": "Ting-Lu",
  "wo-chien": "Wo-Chien",
};

const capitalize = (word: string) => (word ? word[0].toUpperCase() + word.slice(1) : word);

/** Generic slug → words, e.g. `lightning-rod` → `Lightning Rod`. For abilities, items, triggers. */
export function formatSlug(slug: string): string {
  return slug
    .toLowerCase()
    .split("-")
    .filter(Boolean)
    .map(capitalize)
    .join(" ");
}

/** Pokémon name slug → display name, using the exceptions map first. */
export function formatName(slug: string): string {
  const key = slug.toLowerCase();
  return NAME_EXCEPTIONS[key] ?? formatSlug(key);
}
