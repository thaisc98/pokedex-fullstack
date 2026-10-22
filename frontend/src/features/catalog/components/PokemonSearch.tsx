"use client";

import { ComboBox, Description, Input, Label, ListBox, Spinner } from "@heroui/react";
import { useRouter } from "next/navigation";
import { useState, type KeyboardEvent } from "react";
import { formatDexNumber } from "@/shared/lib/format";
import { formatName } from "@/shared/lib/formatName";
import { useDebouncedValue } from "@/shared/lib/useDebouncedValue";
import { useSearchPokemonQuery } from "../catalogApi";
import { exactMatchHref, normalizeSearch } from "../lib/search";

export const SEARCH_DEBOUNCE_MS = 200;

/**
 * "Find a Pokémon": a HeroUI ComboBox (React Aria combobox pattern: arrow keys move through
 * the suggestions, Enter picks one, Esc closes, suggestions are announced). Suggestions come
 * from `GET /pokemon/search` after a short pause in typing. Choosing one, or pressing Enter
 * on an exact name or Pokédex number, opens that Pokémon.
 */
export function PokemonSearch() {
  const router = useRouter();
  const [input, setInput] = useState("");
  const query = normalizeSearch(input);
  const debounced = useDebouncedValue(query, SEARCH_DEBOUNCE_MS);
  const { data, isFetching, isError, error } = useSearchPokemonQuery(debounced ?? "", { skip: !debounced });

  const results = debounced && data ? data.results : [];
  const waiting = query !== debounced || isFetching;
  const unavailable =
    isError && typeof error === "object" && error !== null && "status" in error && (error.status === 503 || error.status === 0);

  const open = (href: string) => router.push(href);

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    // A highlighted suggestion is handled by the ComboBox itself (onSelectionChange).
    if (event.key !== "Enter" || event.currentTarget.getAttribute("aria-activedescendant")) return;
    const href = exactMatchHref(input, results);
    if (href) {
      event.preventDefault();
      open(href);
    }
  };

  return (
    <div className="flex w-full flex-col gap-1 sm:max-w-sm">
      <ComboBox
        fullWidth
        inputValue={input}
        onInputChange={setInput}
        items={results}
        allowsCustomValue
        allowsEmptyCollection
        menuTrigger="input"
        // The server already filtered; don't filter its results again on the client.
        defaultFilter={() => true}
        onSelectionChange={(key) => {
          if (key != null) open(`/pokemon/${key}`);
        }}
      >
        <Label>Find a Pokémon</Label>
        <ComboBox.InputGroup>
          <Input placeholder="Name or number, e.g. pikachu or 25" onKeyDown={onKeyDown} autoComplete="off" />
        </ComboBox.InputGroup>
        <Description>Type a name or Pokédex number.</Description>
        <ComboBox.Popover>
          <ListBox
            renderEmptyState={() =>
              waiting ? (
                <span className="flex items-center gap-2 px-3 py-2 text-sm text-muted">
                  <Spinner size="sm" /> Searching…
                </span>
              ) : unavailable ? (
                <span className="block px-3 py-2 text-sm text-muted">Search is unavailable right now.</span>
              ) : (
                <span className="block px-3 py-2 text-sm text-muted">No Pokémon match</span>
              )
            }
          >
            {(item: { id: number; name: string }) => (
              <ListBox.Item id={item.id} textValue={`${formatDexNumber(item.id)} ${formatName(item.name)}`}>
                <span className="tabular-nums text-muted">{formatDexNumber(item.id)}</span>{" "}
                <span className="font-medium">{formatName(item.name)}</span>
              </ListBox.Item>
            )}
          </ListBox>
        </ComboBox.Popover>
      </ComboBox>
      {unavailable && (
        <p role="status" className="text-sm text-danger">
          Search is unavailable right now. Please try again in a moment.
        </p>
      )}
    </div>
  );
}
