import { describe, expect, it } from "vitest";
import {
  editSchema,
  fieldId,
  fromItem,
  mapServerField,
  normalizeTag,
  toRequest,
  type EditValues,
} from "./editForm";

const empty: EditValues = {
  localizedNames: [],
  region: "",
  habitat: "",
  latitude: undefined,
  longitude: undefined,
  tags: [],
};

const messages = (values: EditValues) => {
  const result = editSchema.safeParse(values);
  return result.success ? {} : Object.fromEntries(result.error.issues.map((i) => [i.path.join("."), i.message]));
};

describe("editSchema", () => {
  it("should_accept_an_empty_form", () => {
    expect(editSchema.safeParse(empty).success).toBe(true);
  });

  it("should_reject_latitude_91", () => {
    expect(messages({ ...empty, latitude: 91, longitude: 0 })).toEqual({
      latitude: "Latitude must be between -90 and 90",
    });
  });

  it("should_require_both_coordinates_or_neither", () => {
    expect(messages({ ...empty, latitude: 10 })).toEqual({
      longitude: "Enter both latitude and longitude, or neither",
    });
  });

  it("should_reject_two_names_in_the_same_language", () => {
    const errors = messages({
      ...empty,
      localizedNames: [
        { lang: "ja", value: "ピカチュウ" },
        { lang: "JA", value: "ぴかちゅう" },
      ],
    });
    expect(errors["localizedNames.1.lang"]).toBe("Only one name per language");
  });

  it("should_reject_a_bad_language_code_and_an_invalid_tag", () => {
    const errors = messages({ ...empty, localizedNames: [{ lang: "japanese!", value: "x" }], tags: ["fire type!"] });
    expect(errors["localizedNames.0.lang"]).toMatch(/language code/);
    expect(errors["tags"]).toMatch(/a–z, 0–9 and -/);
  });

  it("should_reject_more_than_ten_tags", () => {
    const tags = Array.from({ length: 11 }, (_, i) => `t${i}`);
    expect(messages({ ...empty, tags }).tags).toBe("At most 10 tags");
  });
});

describe("normalizeTag", () => {
  it("should_trim_and_lowercase", () => {
    expect(normalizeTag("  Starter ")).toBe("starter");
  });
});

describe("toRequest / fromItem", () => {
  it("should_send_geo_null_when_every_geo_field_is_empty", () => {
    expect(toRequest({ ...empty, tags: ["starter"] }, 3)).toEqual({
      customAttributes: { localizedNames: [], geo: null, tags: ["starter"] },
      version: 3,
    });
  });

  it("should_send_coordinates_and_trimmed_text", () => {
    const body = toRequest(
      { ...empty, localizedNames: [{ lang: " ja ", value: " ピカチュウ " }], region: " Kanto ", latitude: 35.6, longitude: 139.7 },
      0,
    );
    expect(body.customAttributes).toEqual({
      localizedNames: [{ lang: "ja", value: "ピカチュウ" }],
      geo: { region: "Kanto", coordinates: { latitude: 35.6, longitude: 139.7 } },
      tags: [],
    });
  });

  it("should_round_trip_a_collected_item", () => {
    const values = fromItem({
      localizedNames: [{ lang: "de", value: "Pikachu" }],
      geo: { region: "Kanto", habitat: "forest", coordinates: { latitude: 1, longitude: 2 } },
      tags: ["electric"],
    });
    expect(values).toEqual({
      localizedNames: [{ lang: "de", value: "Pikachu" }],
      region: "Kanto",
      habitat: "forest",
      latitude: 1,
      longitude: 2,
      tags: ["electric"],
    });
  });
});

describe("mapServerField", () => {
  it.each([
    ["customAttributes.localizedNames[1].value", "localizedNames.1.value"],
    ["customAttributes.geo.region", "region"],
    ["customAttributes.geo.coordinates.latitude", "latitude"],
    ["customAttributes.geo.coordinates", "latitude"],
    ["customAttributes.tags[2]", "tags"],
    ["customAttributes.tags", "tags"],
    ["version", null],
  ])("should_map_%s_to_%s", (path, expected) => {
    expect(mapServerField(path)).toBe(expected);
  });

  it("should_build_dom_ids_from_field_paths", () => {
    expect(fieldId("localizedNames.1.value")).toBe("edit-localizedNames-1-value");
  });
});
