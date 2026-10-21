import { describe, expect, it } from "vitest";
import { formatName, formatSlug } from "./formatName";

describe("formatName", () => {
  it.each([
    ["pikachu", "Pikachu"],
    ["mr-mime", "Mr. Mime"],
    ["mime-jr", "Mime Jr."],
    ["ho-oh", "Ho-Oh"],
    ["porygon-z", "Porygon-Z"],
    ["porygon2", "Porygon2"],
    ["nidoran-f", "Nidoran♀"],
    ["nidoran-m", "Nidoran♂"],
    ["farfetchd", "Farfetch’d"],
    ["type-null", "Type: Null"],
    ["jangmo-o", "Jangmo-o"],
    ["flabebe", "Flabébé"],
    ["tapu-koko", "Tapu Koko"],
    ["deoxys-normal", "Deoxys Normal"],
  ])("should_format_%s_as_%s", (slug, expected) => {
    expect(formatName(slug)).toBe(expected);
  });

  it("should_be_case_insensitive_for_exceptions", () => {
    expect(formatName("Mr-Mime")).toBe("Mr. Mime");
  });
});

describe("formatSlug", () => {
  it("should_title_case_each_word_of_an_ability_or_item_slug", () => {
    expect(formatSlug("lightning-rod")).toBe("Lightning Rod");
    expect(formatSlug("thunder-stone")).toBe("Thunder Stone");
  });

  it("should_return_an_empty_string_for_an_empty_slug", () => {
    expect(formatSlug("")).toBe("");
  });
});
